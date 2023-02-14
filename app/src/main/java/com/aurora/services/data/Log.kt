package com.aurora.services.data

import android.util.Log

object Log {

    private const val TAG = "¯\\_(ツ)_/¯ "

    fun e(message: String?, vararg args: Any?) {
        e(String.format(message!!, *args))
    }

    fun e(message: String?) {
        Log.e(TAG, message!!)
    }

    fun i(message: String?) {
        Log.i(TAG, message!!)
    }
}
