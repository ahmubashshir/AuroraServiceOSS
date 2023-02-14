package com.aurora.services.data

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager


fun PackageManager.isGranted(packageName: String, permission: String): Boolean {
    return checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED
}

fun Context.isDeviceOwner(packageName: String): Boolean {
    val manager = getSystemService(Service.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return manager.isDeviceOwnerApp(packageName)
}