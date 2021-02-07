package com.aurora.services.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aurora.services.BuildConfig
import com.aurora.services.data.model.App
import com.aurora.services.data.provider.WhitelistProvider
import com.aurora.services.data.utils.extensions.flushAndAdd
import com.aurora.services.data.utils.extensions.toApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope


class WhitelistViewModel(application: Application) : AndroidViewModel(application) {

    private val packageManager: PackageManager = application.packageManager
    private val whitelistProvider: WhitelistProvider = WhitelistProvider.with(application)

    var whitelist: MutableList<App> = mutableListOf()
    var selected: MutableSet<String> = mutableSetOf()

    val liveData: MutableLiveData<List<App>> = MutableLiveData()

    init {
        selected = whitelistProvider.getWhiteList()
        observe()
    }

    fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    val packageInfoMap = getInstallerPackages()
                    val apps = packageInfoMap
                        .filter {
                            it.packageName != null
                                    && it.versionName != null
                                    && it.applicationInfo != null
                                    && it.applicationInfo.enabled
                        }
                        .filter { it.packageName != BuildConfig.APPLICATION_ID }
                        .map { it.toApp(packageManager) }
                        .toList()

                    whitelist.flushAndAdd(apps)
                    liveData.postValue(whitelist.sortedBy { it.displayName })
                } catch (e: Exception) {
                    liveData.postValue(listOf())
                }
            }
        }
    }

    private fun getInstallerPackages(): List<PackageInfo> {
        val packageInfoList: MutableList<PackageInfo> = ArrayList()
        for (packageInfo in packageManager.getInstalledPackages(PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS)) {
            val packageName = packageInfo.packageName

            if (packageInfo.applicationInfo != null
                && !packageInfo.applicationInfo.enabled //Filter Disabled Apps
                || packageManager.getLaunchIntentForPackage(packageName) == null //Filter NonLauncher Apps
            ) continue
            if (packageInfo.requestedPermissions == null)
                continue
            if (!packageInfo.requestedPermissions.contains("android.permission.INSTALL_PACKAGES"))
                continue
            packageInfoList.add(packageInfo)
        }
        return packageInfoList
    }
}