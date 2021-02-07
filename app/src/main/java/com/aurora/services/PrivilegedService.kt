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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.IPackageDeleteObserver
import android.content.pm.IPackageInstallObserver
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.widget.Toast
import com.aurora.services.data.model.Stat
import com.aurora.services.data.provider.StatsProvider
import com.aurora.services.data.utils.Log
import com.aurora.services.data.utils.extensions.isDeviceOwner
import com.aurora.services.data.utils.extensions.isGranted
import org.apache.commons.io.IOUtils
import java.io.*
import java.lang.reflect.Method

class PrivilegedService : Service() {

    private lateinit var statsProvider: StatsProvider
    private lateinit var accessProvider: AccessProvider

    private lateinit var iPrivilegedCallback: IPrivilegedCallback

    private lateinit var installMethod: Method
    private lateinit var deleteMethod: Method

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val returnCode = intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )
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
            val hasInstallPermission = packageManager.isGranted(
                packageName,
                Manifest.permission.INSTALL_PACKAGES
            )

            val hasDeletePermission = packageManager.isGranted(
                packageName,
                Manifest.permission.DELETE_PACKAGES
            )

            return hasInstallPermission && hasDeletePermission
        }

        override fun installPackage(
            packageName: String,
            uri: Uri,
            flags: Int,
            installerPackageName: String,
            callback: IPrivilegedCallback
        ) {
            if (accessProvider.isAllowed()) {
                if (Build.VERSION.SDK_INT >= 21) {
                    createInstallSession(uri)
                    iPrivilegedCallback = callback
                } else {
                    install(
                        uri,
                        flags,
                        installerPackageName,
                        callback
                    )
                }
                updateStats(
                    uri.path.toString(),
                    isGranted = true,
                    isInstall = true
                )
            } else {
                Log.e("Caller is blacklisted")
                updateStats(
                    uri.path.toString(),
                    isGranted = false,
                    isInstall = true
                )
            }
        }

        override fun installSplitPackage(
            packageName: String,
            uriList: List<Uri>,
            flags: Int,
            installerPackageName: String,
            callback: IPrivilegedCallback
        ) {
            if (accessProvider.isAllowed()) {
                createInstallSession(uriList)
                iPrivilegedCallback = callback
                updateStats(uriList[0].path.toString(), true, true)
            } else {
                Log.e("Caller is blacklisted")
                updateStats(uriList[0].path.toString(), false, true)
            }
        }

        override fun deletePackage(
            packageName: String,
            flags: Int,
            callback: IPrivilegedCallback
        ) {
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
                    uninstall(packageName, flags, callback)
                }

                updateStats(
                    packageName,
                    isGranted = true,
                    isInstall = false
                )
            } else {
                updateStats(
                    packageName,
                    isGranted = false,
                    isInstall = false
                )
                Log.e("Caller is blacklisted")
                return
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        accessProvider = AccessProvider.with(this)
        statsProvider = StatsProvider.with(this)

        if (Build.VERSION.SDK_INT < 21) {
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

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
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

    @TargetApi(21)
    private fun createInstallSession(uri: Uri) {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
        val packageInstaller = packageManager.packageInstaller
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        try {
            val outputStream: OutputStream = session.openWrite(
                "PackageInstaller",
                0,
                -1
            )

            IOUtils.copy(
                contentResolver.openInputStream(uri),
                outputStream
            )

            session.fsync(outputStream)

            // Create a PendingIntent and use it to generate the IntentSender
            val installIntent = Intent(Constants.BROADCAST_ACTION_INSTALL)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                sessionId,
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            Log.e("Failure -> %s", e)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        } finally {
            IOUtils.close(session)
        }
    }

    @TargetApi(21)
    private fun createInstallSession(uriList: List<Uri>) {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
        val packageInstaller = packageManager.packageInstaller
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        try {

            for (uri in uriList) {
                val outputStream: OutputStream = session.openWrite(
                    "PackageInstaller",
                    0,
                    -1
                )

                IOUtils.copy(
                    contentResolver.openInputStream(uri),
                    outputStream
                )

                session.fsync(outputStream)
            }

            // Create a PendingIntent and use it to generate the IntentSender
            val installIntent = Intent(Constants.BROADCAST_ACTION_INSTALL)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                sessionId,
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            Log.e("Failure -> %s", e)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        } finally {
            IOUtils.close(session)
        }
    }

    private fun install(
        packageURI: Uri,
        flags: Int,
        installerPackageName: String,
        callback: IPrivilegedCallback
    ) {
        val observer = object : IPackageInstallObserver.Stub() {
            override fun packageInstalled(packageName: String, returnCode: Int) {
                try {
                    callback.handleResult(packageName, returnCode)
                } catch (remoteException: RemoteException) {
                    Log.e("RemoteException -> %s", remoteException)
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

    private fun uninstall(packageName: String, flags: Int, callback: IPrivilegedCallback) {
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
}
