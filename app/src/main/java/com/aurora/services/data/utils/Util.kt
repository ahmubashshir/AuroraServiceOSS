package com.aurora.services.data.utils

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toFile
import java.util.*
import java.util.concurrent.TimeUnit

object Util {

    fun getStyledAttribute(context: Context, styleID: Int): Int {
        val arr = context.obtainStyledAttributes(TypedValue().data, intArrayOf(styleID))
        val styledColor = arr.getColor(0, Color.WHITE)
        arr.recycle()
        return styledColor
    }

    fun getEmptyActivityBundle(context: Context): Bundle? {
        return ActivityOptionsCompat.makeCustomAnimation(
            context,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        ).toBundle()
    }

    fun millisToDayOrTime(millis: Long): String {
        val diff = Calendar.getInstance().timeInMillis - millis
        return when (val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()) {
            0, 1 -> millisToTime(millis)
            else -> "$days days ago"
        }
    }

    private fun millisToTime(millis: Long): String {
        val diff = Calendar.getInstance().timeInMillis - millis
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        val hh = when {
            hours == 1L -> "$hours hr "
            hours > 1L -> "$hours hrs "
            else -> ""
        }

        val mm = when {
            minutes == 1L -> "$minutes minute ago"
            minutes > 1L -> "$minutes minutes ago"
            else -> "Now"
        }

        return "$hh $mm"
    }

    fun getPackageNameFromUri(uri: Uri):String{
        return uri.toFile().name
    }
}