package com.aurora.services.data.utils.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.aurora.services.data.utils.Util


fun <T> Context.open(className: Class<T>) {
    val userAppIntent = Intent(this, className)
    startActivity(
        userAppIntent,
        Util.getEmptyActivityBundle(this)
    )
}

fun AppCompatActivity.setLightConfiguration() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        setLightConfigurationR()
    } else {
        setLightConfigurationO()
    }
}

fun AppCompatActivity.isSystemApp(): Boolean {
    return (applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
}

fun AppCompatActivity.isPermissionGranted(permission: String): Boolean {
    return (ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED)
}

@RequiresApi(Build.VERSION_CODES.R)
private fun AppCompatActivity.setLightConfigurationR() {
    window.insetsController?.setSystemBarsAppearance(
        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
    )
}

private fun AppCompatActivity.setLightConfigurationO() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        setLightStatusBar()
        setLightNavigationBar()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.statusBarColor = ColorUtils.setAlphaComponent(Color.BLACK, 120)
    }
}

private fun AppCompatActivity.setLightStatusBar() {
    var flags = window.decorView.systemUiVisibility
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
    window.decorView.systemUiVisibility = flags
}

private fun AppCompatActivity.setLightNavigationBar() {
    var flags = window.decorView.systemUiVisibility
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        window.navigationBarColor =
            Util.getStyledAttribute(this, android.R.attr.colorBackground)
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
    window.decorView.systemUiVisibility = flags
}

fun AppCompatActivity.close() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        finishAfterTransition()
    } else {
        finish()
    }
}