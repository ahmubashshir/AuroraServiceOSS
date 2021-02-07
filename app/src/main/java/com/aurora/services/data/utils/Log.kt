package com.aurora.services.data.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException

object Log {

    const val TAG = "¯\\_(ツ)_/¯ "

    fun e(message: String?, vararg args: Any?) {
        e(String.format(message!!, *args))
    }

    fun e(message: String?) {
        Log.e(TAG, message!!)
    }

    fun i(message: String?, vararg args: Any?) {
        i(String.format(message!!, *args))
    }

    fun i(message: String?) {
        Log.i(TAG, message!!)
    }

    fun d(message: String?, vararg args: Any?) {
        d(String.format(message!!, *args))
    }

    fun d(message: String?) {
        Log.d(TAG, message!!)
    }

    fun w(message: String?, vararg args: Any?) {
        w(String.format(message!!, *args))
    }

    fun w(message: String?) {
        Log.w(TAG, message!!)
    }

    fun writeToFile(context: Context, obj: Any) {
        try {
            val out = FileWriter(File(context.filesDir, "AuroraLogs.txt"))
            out.write(obj.toString())
            out.close()
        } catch (e: IOException) {
            e(e.message)
        }
    }
}