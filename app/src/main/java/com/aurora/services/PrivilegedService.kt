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
import androidx.annotation.RequiresApi
import com.aurora.services.data.model.Stat
import com.aurora.services.data.provider.AccessProvider
import com.aurora.services.data.provider.StatsProvider
import com.aurora.services.data.utils.Log
import com.aurora.services.data.utils.extensions.isDeviceOwner
import com.aurora.services.data.utils.extensions.isGranted
import org.apache.commons.io.IOUtils
import java.io.*
import java.lang.reflect.Method
import java.util.*


class PrivilegedService : Service() {

    private lateinit var statsProvider: StatsProvider

    private lateinit var iPrivilegedCallback: IPrivilegedCallback

    private lateinit var installMethod: Method
    private lateinit var deleteMethod: Method

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onReceive(context: Context, intent: Intent) {
            val returnCode = intent.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )

            val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
            packageName?.let {
                try {
                    iPrivilegedCallback.handleResult(packageName, returnCode)
                } catch (remoteException: RemoteException) {
                    Log.e("RemoteException -> %s", remoteException)
                }
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

            val isAllowed = AccessProvider
                .with(this@PrivilegedService)
                .isAllowed()

            if (isAllowed) {

                if (Build.VERSION.SDK_INT >= 21) {
                    createInstallSession(packageName, uri)
                    iPrivilegedCallback = callback
                } else {
                    install(packageName, uri, flags, installerPackageName, callback)
                }

                updateStats(
                    packageName,
                    installerPackageName,
                    isGranted = true,
                    isInstall = true
                )
            } else {
                updateStats(
                    packageName,
                    installerPackageName,
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
            val isAllowed = AccessProvider
                .with(this@PrivilegedService)
                .isAllowed()

            if (isAllowed) {
                createInstallSession(packageName, uriList)
                iPrivilegedCallback = callback
                updateStats(packageName, installerPackageName, isGranted = true, isInstall = true)
            } else {
                updateStats(packageName, installerPackageName, isGranted = false, isInstall = true)
            }
        }

        override fun deletePackage(
            packageName: String,
            flags: Int,
            callback: IPrivilegedCallback
        ) {
            val isAllowed = AccessProvider
                .with(this@PrivilegedService)
                .isAllowed()

            if (isAllowed) {
                if (Build.VERSION.SDK_INT >= 24) {
                    iPrivilegedCallback = callback

                    val packageManager = packageManager
                    val packageInstaller = packageManager.packageInstaller

                    packageManager.setInstallerPackageName(
                        packageName,
                        BuildConfig.APPLICATION_ID
                    )

                    val intent = Intent(Constants.BROADCAST_ACTION_UNINSTALL)
                    val pendingIntent = PendingIntent.getBroadcast(
                        this@PrivilegedService,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    packageInstaller.uninstall(packageName, pendingIntent.intentSender)
                } else {
                    uninstall(packageName, flags, callback)
                }

                updateStats(packageName, "", isGranted = true, isInstall = false)
            } else {
                updateStats(packageName, "", isGranted = false, isInstall = false)
                return
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

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
    private fun createInstallSession(packageName: String, uri: Uri) {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
        val packageInstaller = packageManager.packageInstaller
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        try {

            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = session.openWrite(
                packageName,
                0,
                -1
            )

            IOUtils.copy(inputStream, outputStream)

            session.fsync(outputStream)

            IOUtils.close(inputStream)
            IOUtils.close(outputStream)

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
        } finally {
            IOUtils.close(session)
        }
    }

    @TargetApi(21)
    private fun createInstallSession(packageName: String, uriList: List<Uri>) {
        val packageInstaller = packageManager.packageInstaller
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        try {
            var apkId = 1
            for (uri in uriList) {
                val inputStream = contentResolver.openInputStream(uri)
                val outputStream = session.openWrite(
                    "${packageName}_${apkId++}",
                    0,
                    -1
                )

                IOUtils.copy(inputStream, outputStream)

                session.fsync(outputStream)

                IOUtils.close(inputStream)
                IOUtils.close(outputStream)
            }


            val intent = Intent(Constants.BROADCAST_ACTION_INSTALL)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            Log.e("Failure -> %s", e)
        } finally {
            IOUtils.close(session)
        }
    }

    private fun install(
        packageName: String, packageURI: Uri, flags: Int,
        installerPackageName: String,
        callback: IPrivilegedCallback
    ) {
        val observer = object : IPackageInstallObserver.Stub() {
            override fun packageInstalled(packageName: String?, returnCode: Int) {
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
                callback.handleResult(packageName, 0)
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
            override fun packageDeleted(packageName: String?, returnCode: Int) {
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
                callback.handleResult(packageName, 0)
            } catch (e1: RemoteException) {
                Log.e("RemoteException -> %s", e1)
            }
        }
    }

    private fun updateStats(
        packageName: String,
        callerPackageName: String,
        isGranted: Boolean = false,
        isInstall: Boolean = false
    ) {
        statsProvider.add(
            Stat(packageName).apply {
                granted = isGranted
                timeStamp = System.currentTimeMillis()
                installerPackageName = callerPackageName
                install = isInstall
            }
        )
    }
}
