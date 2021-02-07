package com.aurora.services.data.utils.extensions

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.aurora.services.data.model.App

fun PackageInfo.toApp(packageManager: PackageManager): App {
    val pi = this
    return App(packageName).apply {
        displayName = packageManager.getApplicationLabel(pi.applicationInfo).toString()
        versionCode = pi.versionCode
        versionName = pi.versionName
        drawable = packageManager.getApplicationIcon(pi.packageName)
    }
}