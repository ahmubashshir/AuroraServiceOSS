package com.aurora.services

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import com.aurora.services.data.provider.WhitelistProvider
import com.aurora.services.data.utils.Log

class AccessProvider private constructor(var context: Context) {

    companion object : SingletonHolder<AccessProvider, Context>(::AccessProvider)

    private val packageManager: PackageManager = context.packageManager

    fun getInstallerPackageName(): String {
        val uid = Binder.getCallingUid()
        val callingPackages = packageManager.getPackagesForUid(uid)
        return if (callingPackages == null)
            "Unknown"
        else
            callingPackages[0]
    }

    fun isAllowed(): Boolean {
        return isUidAllowed(Binder.getCallingUid())
    }

    private fun isUidAllowed(uid: Int): Boolean {
        val callingPackages = packageManager.getPackagesForUid(uid)
            ?: throw RuntimeException("No packages associated to caller UID!")
        val currentPkg = callingPackages[0]
        return isPackageAllowed(currentPkg)
    }

    private fun isPackageAllowed(packageName: String): Boolean {
        Log.i("Checking if package is allowed to access Aurora Services: %s", packageName)
        return if (WhitelistProvider.with(context).isWhitelisted(packageName)) {
            Log.i("Package is allowed to access Aurora Services")
            true
        } else {
            Log.e("Package is NOT allowed to access Aurora Services")
            false
        }
    }
}