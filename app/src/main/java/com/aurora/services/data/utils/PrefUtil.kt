package com.aurora.services.data.utils

import android.content.Context
import android.content.SharedPreferences
import com.aurora.services.BuildConfig

object PrefUtil {

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
    }

    fun putString(context: Context, key: String, value: String) {
        getPrefs(context.applicationContext).edit().putString(key, value).apply()
    }

    fun putInteger(context: Context, key: String, value: Int) {
        getPrefs(context.applicationContext).edit().putInt(key, value).apply()
    }

    fun putFloat(context: Context, key: String, value: Float) {
        getPrefs(context.applicationContext).edit().putFloat(key, value).apply()
    }

    fun putLong(context: Context, key: String, value: Long) {
        getPrefs(context.applicationContext).edit().putLong(key, value).apply()
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context.applicationContext).edit().putBoolean(key, value).apply()
    }

    fun getString(context: Context, key: String): String {
        return getPrefs(context.applicationContext).getString(key, "").toString()
    }

    fun getInteger(context: Context, key: String): Int {
        return getPrefs(context.applicationContext).getInt(key, 0)
    }

    fun getFloat(context: Context, key: String): Float {
        return getPrefs(context.applicationContext).getFloat(key, 0.0f)
    }

    fun getLong(context: Context, key: String): Long {
        return getPrefs(context.applicationContext).getLong(key, 0L)
    }

    fun getBoolean(context: Context, key: String): Boolean {
        return getPrefs(context.applicationContext).getBoolean(key, false)
    }
}