package com.aurora.services.data.provider

import android.content.Context
import com.aurora.services.SingletonHolder
import com.aurora.services.data.utils.PrefUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Modifier

class WhitelistProvider private constructor(var context: Context) {

    companion object : SingletonHolder<WhitelistProvider, Context>(::WhitelistProvider) {
        const val PREFERENCE_WHITELIST = "PREFERENCE_WHITELIST"
    }

    private var gson: Gson = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT)
        .create()

    fun getWhiteList(): MutableSet<String> {
        val rawBlacklist = PrefUtil.getString(context, PREFERENCE_WHITELIST)
        return try {
            if (rawBlacklist.isEmpty())
                mutableSetOf()
            else
                gson.fromJson(rawBlacklist, object : TypeToken<Set<String?>?>() {}.type)
        } catch (e: Exception) {
            mutableSetOf()
        }
    }

    fun isWhitelisted(packageName: String): Boolean {
        return getWhiteList().contains(packageName)
    }

    fun whitelist(packageName: String) {
        val oldBlackList: MutableSet<String> = getWhiteList()
        oldBlackList.add(packageName)
        save(oldBlackList)
    }

    fun whitelist(packageNames: Set<String>) {
        val oldBlackList: MutableSet<String> = getWhiteList()
        oldBlackList.addAll(packageNames)
        save(oldBlackList)
    }

    @Synchronized
    fun save(whitelist: Set<String>) {
        PrefUtil.putString(context, PREFERENCE_WHITELIST, gson.toJson(whitelist))
    }
}