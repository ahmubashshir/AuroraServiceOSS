/*
 * Copyright (C) 2015-2016 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright 2007, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aurora.services

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.IPackageDeleteObserver
import android.content.pm.IPackageInstallObserver
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.widget.Toast
import com.aurora.services.data.model.Stat
import com.aurora.services.data.provider.StatsProvider
import com.aurora.services.data.utils.CommonUtils
import com.aurora.services.data.utils.IOUtils
import com.aurora.services.data.utils.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Method

class PrivilegedService : Service() {

    private lateinit var statsProvider: StatsProvider
    private lateinit var accessProvider: AccessProvider

    private lateinit var iPrivilegedCallback: IPrivilegedCallback

    private lateinit var installMethod: Method
    private lateinit var deleteMethod: Method

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val returnCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
            try {
                iPrivilegedCallback.handleResult(packageName, returnCode)
            } catch (e1: RemoteException) {
                Log.e("RemoteException -> %s", e1)
            }
        }
    }

    private val binder: IPrivilegedService.Stub = object : IPrivilegedService.Stub() {

        override fun hasPrivilegedPermissions(): Boolean {
            val callerIsAllowed = accessProvider.isAllowed()
            return callerIsAllowed && hasPrivilegedPermissionsImpl()
        }

        override fun installPackage(
            packageURI: Uri,
            flags: Int,
            installerPackageName: String,
            callback: IPrivilegedCallback
        ) {
            if (accessProvider.isAllowed()) {
                if (Build.VERSION.SDK_INT >= 24) {
                    doPackageStage(packageURI)
                    iPrivilegedCallback = callback
                } else {
                    this@PrivilegedService.installPackage(
                        packageURI,
                        flags,
                        installerPackageName,
                        callback
                    )
                }
            } else {
                Log.e("Caller is blacklisted")
                return
            }
        }

        override fun installSplitPackage(
            uriList: List<Uri>, flags: Int, installerPackageName: String,
            callback: IPrivilegedCallback
        ) {
            if (accessProvider.isAllowed()) {
                doSplitPackageStage(uriList)
                iPrivilegedCallback = callback
            } else {
                Log.e("Caller is blacklisted")
                return
            }
        }

        override fun deletePackage(packageName: String, flags: Int, callback: IPrivilegedCallback) {
            if (accessProvider.isAllowed()) {
                if (Build.VERSION.SDK_INT >= 24) {
                    iPrivilegedCallback = callback
                    val packageManager = packageManager
                    val packageInstaller = packageManager.packageInstaller
                    packageManager.setInstallerPackageName(packageName, "com.aurora.services")
                    val uninstallIntent = Intent(Constants.BROADCAST_ACTION_UNINSTALL)
                    val pendingIntent = PendingIntent.getBroadcast(
                        this@PrivilegedService,
                        0,
                        uninstallIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    packageInstaller.uninstall(packageName, pendingIntent.intentSender)
                } else {
                    this@PrivilegedService.deletePackage(packageName, flags, callback)
                }
            } else {
                Log.e("Caller is blacklisted")
                return
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        accessProvider = AccessProvider.with(this)
        statsProvider = StatsProvider.with(this)

        if (Build.VERSION.SDK_INT < 24) {
            try {
                registerMethods()
            } catch (e: NoSuchMethodException) {
                Log.e("Android not compatible! -> %s", e.message)
                stopSelf()
            }
        }

        registerReceivers()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun registerMethods() {
        val installTypes = arrayOf(
            Uri::class.java,
            IPackageInstallObserver::class.java,
            Int::class.javaPrimitiveType,
            String::class.java
        )

        val deleteTypes = arrayOf(
            String::class.java, IPackageDeleteObserver::class.java,
            Int::class.javaPrimitiveType
        )

        installMethod = packageManager.javaClass.getMethod(
            "installPackage",
            *installTypes
        )

        deleteMethod = packageManager.javaClass.getMethod(
            "deletePackage",
            *deleteTypes
        )
    }

    private fun registerReceivers() {
        val installIntent = IntentFilter().apply {
            addAction(Constants.BROADCAST_ACTION_INSTALL)
        }

        val uninstallIntent = IntentFilter().apply {
            addAction(Constants.BROADCAST_ACTION_UNINSTALL)
        }

        registerReceiver(
            broadcastReceiver,
            installIntent,
            Constants.BROADCAST_SENDER_PERMISSION,
            null
        )

        registerReceiver(
            broadcastReceiver,
            uninstallIntent,
            Constants.BROADCAST_SENDER_PERMISSION,
            null
        )
    }

    @TargetApi(24)
    private fun doPackageStage(uri: Uri) {
        val packageManager = packageManager
        val packageInstaller = packageManager.packageInstaller
        val params = SessionParams(
            SessionParams.MODE_FULL_INSTALL
        )
        try {
            val buffer = ByteArray(65536)
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            val file = File(uri.path!!)
            val inputStream: InputStream = FileInputStream(file)
            val outputStream = session.openWrite(
                "PackageInstaller",
                0,
                -1
            )

            try {
                var len: Int
                while (inputStream.read(buffer).also { len = it } != -1) {
                    outputStream.write(buffer, 0, len)
                }
                session.fsync(outputStream)
            } finally {
                CommonUtils.closeQuietly(inputStream)
                CommonUtils.closeQuietly(outputStream)
            }

            // Create a PendingIntent and use it to generate the IntentSender
            val installIntent = Intent(Constants.BROADCAST_ACTION_INSTALL)
            val pendingIntent = PendingIntent.getBroadcast(
                this /*context*/,
                sessionId,
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            session.commit(pendingIntent.intentSender)
            CommonUtils.closeQuietly(session)
        } catch (e: IOException) {
            Log.e("Failure -> %s", e)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }

    @TargetApi(21)
    private fun doSplitPackageStage(uriList: List<Uri>) {
        val packageManager = packageManager
        val packageInstaller = packageManager.packageInstaller
        try {
            val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            for (uri in uriList) {
                val file = File(uri.path!!)
                val inputStream: InputStream = FileInputStream(file)
                val outputStream = session.openWrite(file.name, 0, file.length())
                IOUtils.copy(inputStream, outputStream)
                session.fsync(outputStream)
                CommonUtils.closeQuietly(inputStream)
                CommonUtils.closeQuietly(outputStream)
            }
            val installIntent = Intent(Constants.BROADCAST_ACTION_INSTALL)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                sessionId,
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            session.commit(pendingIntent.intentSender)
            CommonUtils.closeQuietly(session)
        } catch (e: IOException) {
            Log.e("Failure -> %s", e)
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun hasPrivilegedPermissionsImpl(): Boolean {
        val hasInstallPermission = (packageManager
            .checkPermission(Manifest.permission.INSTALL_PACKAGES, packageName)
                == PackageManager.PERMISSION_GRANTED)
        val hasDeletePermission = (packageManager
            .checkPermission(Manifest.permission.DELETE_PACKAGES, packageName)
                == PackageManager.PERMISSION_GRANTED)
        return hasInstallPermission && hasDeletePermission
    }

    private fun installPackage(
        packageURI: Uri,
        flags: Int,
        installerPackageName: String,
        callback: IPrivilegedCallback
    ) {
        val observer = object : IPackageInstallObserver.Stub() {
            override fun packageInstalled(packageName: String, returnCode: Int) {
                try {
                    callback.handleResult(packageName, returnCode)
                } catch (e1: RemoteException) {
                    Log.e("RemoteException -> %s", e1)
                }
            }
        }

        try {
            installMethod.invoke(
                packageManager,
                packageURI,
                observer,
                flags,
                installerPackageName
            )
        } catch (e: Exception) {
            Log.e("Android not compatible! -> %s", e)
            try {
                callback.handleResult(null, 0)
            } catch (remoteException: RemoteException) {
                Log.e("RemoteException -> %s", remoteException)
            }
        }
    }

    private fun deletePackage(packageName: String, flags: Int, callback: IPrivilegedCallback) {
        if (isDeviceOwner(packageName)) {
            Log.e("Cannot delete %s. This app is the device owner.", packageName)
            return
        }

        val observer = object : IPackageDeleteObserver.Stub() {
            override fun packageDeleted(packageName: String, returnCode: Int) {
                try {
                    callback.handleResult(packageName, returnCode)
                } catch (remoteException: RemoteException) {
                    Log.e("RemoteException -> %s", remoteException)
                }
            }
        }

        // execute internal method
        try {
            deleteMethod.invoke(packageManager, packageName, observer, flags)
        } catch (e: Exception) {
            Log.e("Android not compatible! -> %s", e)
            try {
                callback.handleResult(null, 0)
            } catch (e1: RemoteException) {
                Log.e("RemoteException -> %s", e1)
            }
        }
    }

    private fun isDeviceOwner(packageName: String): Boolean {
        val manager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return manager.isDeviceOwnerApp(packageName)
    }

    private fun updateStats(
        packageName: String,
        isGranted: Boolean = false,
        isInstall: Boolean = false
    ) {
        statsProvider.add(
            Stat(packageName).apply {
                granted = isGranted
                timeStamp = System.currentTimeMillis()
                installerPackageName = accessProvider.getInstallerPackageName()
                install = isInstall
            }
        )
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }
}