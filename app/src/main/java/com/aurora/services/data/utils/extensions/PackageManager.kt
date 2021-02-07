package com.aurora.services.data.utils.extensions

import android.content.pm.PackageManager

fun PackageManager.isGranted(packageName: String, permission: String): Boolean {
    return checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED
}