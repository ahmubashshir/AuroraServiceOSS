package com.aurora.services

object Constants {
    const val SHARED_PREFERENCES_KEY = "com.aurora.services"
    const val TAG = "Aurora Service"
    const val BROADCAST_ACTION_INSTALL = "com.aurora.services.ACTION_INSTALL_COMMIT"
    const val BROADCAST_ACTION_UNINSTALL = "com.aurora.services.ACTION_UNINSTALL_COMMIT"
    const val BROADCAST_SENDER_PERMISSION = "android.permission.INSTALL_PACKAGES"
    const val PREFERENCE_WHITELIST_PACKAGE_LIST = "PREFERENCE_WHITELIST_PACKAGE_LIST"
    const val PREFERENCE_STATS_LIST = "PREFERENCE_STATS_LIST"
}