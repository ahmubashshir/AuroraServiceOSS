package com.aurora.services.data.model

data class Dash(
    val id: Int,
    var title: String,
    var subtitle: String,
    var icon: Int = 0
) {

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Dash -> other.id == id
            else -> false
        }
    }
}