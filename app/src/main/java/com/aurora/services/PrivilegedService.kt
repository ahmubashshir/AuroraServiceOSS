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
import android.annotation.SuppressLint
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
import androidx.core.net.toFile
import com.aurora.services.data.AccessProvider
import com.aurora.services.data.Log
import com.aurora.services.data.isDeviceOwner
import com.aurora.services.data.isGranted
import org.apache.commons.io.IOUtils
import java.io.*
import java.lang.reflect.Method
import java.util.*

class PrivilegedService : Service() {

    private lateinit var iPrivilegedCallback: IPrivilegedCallback
    private lateinit var installMethod: Method
    private lateinit var deleteMethod: Method

    private var installerPackageName: String = "Unknown"
    private var installerType: Int = 0

    companion object {
        const val DELETE_FAILED = -1
        const val DELETE_FAILED_OWNER = -4

        const val ACTION_INSTALL = "com.aurora.services.ACTION_INSTALL"
        const val ACTION_UNINSTALL = "com.aurora.services.ACTION_UNINSTALL"
        fun getPackageNameFromUri(uri: Uri):String{
            return uri.toFile().name
        }
        val INTENT_FLAGS = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onReceive(context: Context, intent: Intent) {
            val returnCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -69)
            val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
            val extra = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

            Log.i("Callback -> $installerPackageName $packageName $returnCode $extra")

            packageName?.let {

                try {
                    iPrivilegedCallback.handleResult(packageName, returnCode)
                    iPrivilegedCallback.handleResultX(packageName, returnCode, extra)
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
            packageURI: Uri,
            flags: Int,
            installerPackageName: String,
            callback: IPrivilegedCallback
        ) {
            val packageName = getPackageNameFromUri(packageURI)
            installPackageX(packageName, packageURI, flags, installerPackageName, callback)
        }

        override fun installSplitPackage(
            listURI: List<Uri>,
            flags: Int,
            installerPackageName: String,
            callback: IPrivilegedCallback
        ) {
            val packageName = getPackageNameFromUri(listURI[0])
            installSplitPackageX(packageName, listURI, flags, installerPackageName, callback)
        }

        override fun installPackageX(
            packageName: String,
            uri: Uri,
            flags: Int,
            installerPackageName: String,
            callback: IPrivilegedCallback
        ) {
            val isAllowed = AccessProvider(this@PrivilegedService).isAllowed()

            if (isAllowed) {
                if (Build.VERSION.SDK_INT >= 21) {
                    createInstallSession(packageName, installerPackageName, uri, callback)
                } else {
                    install(packageName, uri, flags, installerPackageName, callback)
                }
            } else {
                handleFailure(
                    callback,
                    packageName,
                    1,
                    "Installer not allowed"
                )
            }
        }

        override fun installSplitPackageX(
            packageName: String,
            uriList: List<Uri>,
            flags: Int,
            installerPackageName: String,
            callback: IPrivilegedCallback
        ) {
            val isAllowed = AccessProvider(this@PrivilegedService).isAllowed()

            if (isAllowed) {
                createInstallSession(packageName, installerPackageName, uriList, callback)
            } else {
                handleFailure(
                    callback,
                    packageName,
                    1,
                    "Installer not allowed"
                )
            }
        }

        override fun deletePackage(
            packageName: String,
            flags: Int,
            callback: IPrivilegedCallback
        ) {
            deletePackageX(packageName, flags, "¯\\_(ツ)_/¯", callback)
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        override fun deletePackageX(
            packageName: String,
            flags: Int,
            installerPackageName: String,
            callback: IPrivilegedCallback
        ) {
            this@PrivilegedService.iPrivilegedCallback = callback
            this@PrivilegedService.installerPackageName = installerPackageName
            this@PrivilegedService.installerType = 1

            val isAllowed = AccessProvider(this@PrivilegedService).isAllowed()

            if (isAllowed) {
                try {
                    if (Build.VERSION.SDK_INT >= 24) {
                        val packageManager = packageManager
                        val packageInstaller = packageManager.packageInstaller

                        packageManager.setInstallerPackageName(
                            packageName,
                            BuildConfig.APPLICATION_ID
                        )

                        val intent = Intent(ACTION_UNINSTALL)
                        val pendingIntent = PendingIntent.getBroadcast(
                            this@PrivilegedService,
                            0,
                            intent,
                            INTENT_FLAGS
                        )

                        packageInstaller.uninstall(packageName, pendingIntent.intentSender)
                    } else {
                        uninstall(packageName, installerPackageName, flags, callback)
                    }

                } catch (e: Exception) {
                    Log.e("Error: ${e.message}")
                    handleFailure(
                        callback,
                        packageName,
                        DELETE_FAILED,
                        e.stackTraceToString()
                    )
                }
            } else {
                handleFailure(
                    callback,
                    packageName,
                    DELETE_FAILED,
                    "Un-installer now allowed"
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()


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

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceivers() {
        val installIntent = IntentFilter().apply {
            addAction(ACTION_INSTALL)
            addAction(ACTION_UNINSTALL)
        }
        if (Build.VERSION.SDK_INT >= 33)
            registerReceiver(
                broadcastReceiver,
                installIntent,
                Manifest.permission.INSTALL_PACKAGES,
                null,
                RECEIVER_EXPORTED
            )
        else
            registerReceiver(
                broadcastReceiver,
                installIntent,
                Manifest.permission.INSTALL_PACKAGES,
                null
            )
    }

    @TargetApi(21)
    private fun createInstallSession(
        packageName: String,
        installerPackageName: String,
        uri: Uri,
        callback: IPrivilegedCallback
    ) {
        this.iPrivilegedCallback = callback
        this.installerPackageName = installerPackageName
        this.installerType = 0

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
            val intent = Intent(ACTION_INSTALL)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                sessionId,
                intent,
                INTENT_FLAGS
            )

            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            Log.e("Failure -> %s", e)
            callback.handleResultX(
                packageName,
                PackageInstaller.STATUS_FAILURE,
                e.stackTraceToString()
            )

            callback.handleResult(
                packageName,
                PackageInstaller.STATUS_FAILURE
            )
        } finally {
            IOUtils.close(session)
        }
    }

    @TargetApi(21)
    private fun createInstallSession(
        packageName: String,
        installerPackageName: String,
        uriList: List<Uri>,
        callback: IPrivilegedCallback
    ) {
        this.iPrivilegedCallback = callback
        this.installerPackageName = installerPackageName
        this.installerType = 0

        val packageInstaller = packageManager.packageInstaller
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL)

        try {

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

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

            val intent = Intent(ACTION_INSTALL)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                sessionId,
                intent,
                INTENT_FLAGS
            )

            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            Log.e("Error -> ${e.message}")
            handleFailure(
                callback,
                packageName,
                1,
                e.stackTraceToString()
            )
        }
    }

    private fun install(
        packageName: String,
        packageURI: Uri,
        flags: Int,
        installerPackageName: String,
        callback: IPrivilegedCallback
    ) {
        this.iPrivilegedCallback = callback
        this.installerPackageName = installerPackageName
        this.installerType = 0

        val observer = object : IPackageInstallObserver.Stub() {
            override fun packageInstalled(packageName: String?, returnCode: Int) {
                try {
                    callback.handleResult(packageName, returnCode)
                    callback.handleResultX(
                        packageName,
                        returnCode,
                        "Apps installed successfully"
                    )
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
            Log.e("Error : ${e.message}")
            handleFailure(
                callback,
                packageName,
                1,
                e.stackTraceToString()
            )
        }
    }

    private fun uninstall(
        packageName: String,
        installerPackageName: String,
        flags: Int,
        callback: IPrivilegedCallback
    ) {
        this.iPrivilegedCallback = callback
        this.installerPackageName = installerPackageName
        this.installerType = 1

        if (isDeviceOwner(packageName)) {
            val error = "Cannot delete $packageName. This app is the device owner."
            Log.e(error)
            handleFailure(
                callback,
                packageName,
                DELETE_FAILED_OWNER,
                error
            )
        } else {
            val observer = object : IPackageDeleteObserver.Stub() {
                override fun packageDeleted(packageName: String?, returnCode: Int) {
                    try {
                        callback.handleResultX(
                            packageName,
                            returnCode,
                            "App uninstalled"
                        )

                        callback.handleResult(
                            packageName,
                            returnCode
                        )
                    } catch (remoteException: RemoteException) {
                        Log.e("RemoteException -> %s", remoteException)
                        packageName?.let {
                            handleFailure(
                                callback,
                                it,
                                -1,
                                remoteException.stackTraceToString()
                            )
                        }
                    }
                }
            }

            try {
                deleteMethod.invoke(packageManager, packageName, observer, flags)
            } catch (e: Exception) {
                Log.e("Error : ${e.message}")
                handleFailure(callback, packageName, -1, "")
            }
        }
    }

    private fun handleFailure(
        callback: IPrivilegedCallback,
        packageName: String,
        code: Int,
        extra: String
    ) {
        callback.handleResultX(
            packageName,
            code,
            extra
        )

        callback.handleResult(
            packageName,
            code
        )

    }
}
