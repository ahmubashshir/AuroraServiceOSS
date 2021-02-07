package com.aurora.services.view.ui.activities

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.aurora.services.R
import com.aurora.services.data.model.Stat
import com.aurora.services.databinding.ActivityLogBinding
import com.aurora.services.view.epoxy.StatViewModel_
import com.aurora.services.viewmodel.StatisticsViewModel

class StatisticsActivity : BaseActivity() {

    private lateinit var B: ActivityLogBinding
    private lateinit var VM: StatisticsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityLogBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this).get(StatisticsViewModel::class.java)

        setContentView(B.root)

        attachToolbar()

        VM.liveData.observe(this, {
            updateController(it)
        })
    }

    private fun attachToolbar() {
        setSupportActionBar(B.layoutToolbar.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f
            actionBar.setTitle(R.string.title_log)
        }
    }

    private fun updateController(blackList: List<Stat>?) {
        B.recycler.withModels {
            setFilterDuplicates(true)
            if (blackList == null) {
                for (i in 1..6) {
                    /*add(
                        AppListViewShimmerModel_()
                            .id(i)
                    )*/
                }
            } else {
                blackList.forEach {
                    add(
                        StatViewModel_()
                            .id(it.packageName.hashCode())
                            .app(it)
                    )
                }
            }
        }
    }
}