package com.aurora.services.data.provider

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import com.aurora.services.SingletonHolder

class AccessProvider private constructor(var context: Context) {

    companion object : SingletonHolder<AccessProvider, Context>(::AccessProvider) {
        const val PACKAGE_AURORA_STORE = "com.aurora.store"
        const val PACKAGE_AURORA_STORE_BETA = "com.aurora.store.beta"
        const val PACKAGE_AURORA_DROID = "com.aurora.droid"
    }

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
        return packageName == PACKAGE_AURORA_STORE
                || packageName == PACKAGE_AURORA_STORE_BETA
                || packageName == PACKAGE_AURORA_DROID
    }
}