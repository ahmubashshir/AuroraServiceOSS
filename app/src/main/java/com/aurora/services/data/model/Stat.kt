package com.aurora.services.data.model

data class Stat(val packageName: String) {
    var installerPackageName: String = "Unknown"
    var timeStamp: Long = 0L
    var granted = false
    var install = true

    override fun hashCode(): Int {
        return packageName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Stat -> other.packageName == packageName && install == other.install
            else -> false
        }
    }
}