package com.aurora.services.data.provider

import android.content.Context
import com.aurora.services.data.model.Stat
import com.aurora.services.data.utils.PrefUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Modifier

class StatsProvider constructor(var context: Context) {

    companion object {
        const val PREFERENCE_STATS = "PREFERENCE_STATS"
    }

    private var gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT)
        .create()

    fun getStats(): MutableList<Stat> {
        val rawLog = PrefUtil.getString(context, PREFERENCE_STATS)
        return try {
            if (rawLog.isEmpty())
                mutableListOf()
            else
                gson.fromJson(rawLog, object : TypeToken<MutableList<Stat?>?>() {}.type)
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun add(packageName: Stat) {
        val oldBlackList: MutableList<Stat> = getStats()
        oldBlackList.add(packageName)
        save(oldBlackList)
    }

    fun clear() {
        PrefUtil.putString(context, PREFERENCE_STATS, "{}")
    }

    @Synchronized
    fun save(logs: List<Stat>) {
        PrefUtil.putString(context, PREFERENCE_STATS, gson.toJson(logs))
    }
}