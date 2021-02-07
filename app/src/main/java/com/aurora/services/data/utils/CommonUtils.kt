package com.aurora.services.data.utils

import android.content.Context
import android.content.SharedPreferences
import com.aurora.services.BuildConfig
import java.io.Closeable
import java.io.IOException

object CommonUtils {

    fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (ignored: IOException) {
        }
    }

    fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}