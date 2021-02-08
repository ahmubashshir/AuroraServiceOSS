package com.aurora.services.view.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.aurora.services.R
import com.aurora.services.data.model.Stat
import com.aurora.services.databinding.ActivityStatisticsBinding
import com.aurora.services.view.epoxy.NoAppViewModel_
import com.aurora.services.view.epoxy.StatViewModel_
import com.aurora.services.viewmodel.StatisticsViewModel

class StatisticsActivity : BaseActivity() {

    private lateinit var B: ActivityStatisticsBinding
    private lateinit var VM: StatisticsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityStatisticsBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this).get(StatisticsViewModel::class.java)

        setContentView(B.root)

        VM.liveData.observe(this, {
            updateController(it)
        })

        attachToolbar()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.statistics_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_clear -> {
                VM.clear()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun attachToolbar() {
        setSupportActionBar(B.layoutToolbar.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f
            actionBar.setTitle(R.string.title_statistics)
        }
    }

    private fun updateController(statistics: List<Stat>) {
        B.recycler.withModels {
            setFilterDuplicates(true)
            if (statistics.isEmpty()) {
                add(
                    NoAppViewModel_()
                        .id("no_app")
                        .message(getString(R.string.title_no_statistics))
                )
            } else {
                statistics.forEach {
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