package com.aurora.services.data.utils.extensions

fun <T> MutableList<T>.flushAndAdd(list: List<T>) {
    clear()
    addAll(list)
}

fun <T> MutableSet<T>.flushAndAdd(list: Set<T>) {
    clear()
    addAll(list)
}