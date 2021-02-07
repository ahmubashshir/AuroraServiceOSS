package com.aurora.services.data.utils

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import androidx.core.app.ActivityOptionsCompat
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

    fun millisToDay(millis: Long): String {
        val diff = Calendar.getInstance().timeInMillis - millis
        return when (val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> days.toString() + "ago"
        }
    }

    fun millisToTime(millis: Long): StringBuilder {
        val diff = Calendar.getInstance().timeInMillis - millis
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val hh = if (hours >= 1) hours.toString() + "hr" else ""
        return StringBuilder()
            .append(if (hh.isEmpty()) "" else hh)
            .append(" ")
            .append(minutes)
            .append(" ")
            .append(if (minutes >= 1) "minutes ago" else "minute ago")
    }
}