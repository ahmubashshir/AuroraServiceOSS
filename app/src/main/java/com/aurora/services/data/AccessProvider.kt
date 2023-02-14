package com.aurora.services.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder

class AccessProvider(context: Context) {

    private val allowedPackages: MutableSet<String> = mutableSetOf(
        "com.aurora.store",
        "com.aurora.store.debug",
        "com.aurora.store.nightly"
    )

    private val packageManager: PackageManager = context.packageManager

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
        val isAllowed = allowedPackages.contains(packageName)
        if (isAllowed)
            Log.i("Caller Allowed : $packageName")
        else
            Log.e("Caller not allowed : $packageName")
        return isAllowed
    }
}
