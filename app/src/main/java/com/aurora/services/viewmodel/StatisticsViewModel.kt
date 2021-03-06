package com.aurora.services.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aurora.services.data.model.Stat
import com.aurora.services.data.provider.StatsProvider
import com.aurora.services.data.utils.extensions.flushAndAdd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val statsProvider: StatsProvider = StatsProvider(application)

    var stats: MutableList<Stat> = mutableListOf()
    val liveData: MutableLiveData<List<Stat>> = MutableLiveData()

    init {
        observe()
    }

    fun clear() {
        statsProvider.clear()
        observe()
    }

    private fun observe() {
        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                try {
                    stats.flushAndAdd(statsProvider.getStats())
                    liveData.postValue(statsProvider.getStats().sortedByDescending { it.timeStamp })
                } catch (e: Exception) {
                    liveData.postValue(listOf())
                }
            }
        }
    }
}