package com.aurora.services.data.model

import android.graphics.drawable.Drawable

data class App(val packageName: String) {
    var displayName: String = String()
    var versionName: String = String()
    var installer: String = String()
    var installLocation: String = String()
    var iconBase64: String = String()
    var installedTime: Long = 0L
    var versionCode: Int = 0
    var drawable:Drawable? =null

    override fun hashCode(): Int {
        return packageName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is App -> other.packageName == packageName
            else -> false
        }
    }
}