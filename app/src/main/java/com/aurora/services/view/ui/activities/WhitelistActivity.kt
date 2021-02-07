package com.aurora.services.view.ui.activities

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.aurora.services.R
import com.aurora.services.data.model.App
import com.aurora.services.data.provider.WhitelistProvider
import com.aurora.services.databinding.ActivityWhitelistBinding
import com.aurora.services.view.epoxy.AppViewModel_
import com.aurora.services.viewmodel.WhitelistViewModel

class WhitelistActivity : BaseActivity() {

    private lateinit var B: ActivityWhitelistBinding
    private lateinit var VM: WhitelistViewModel
    private lateinit var whitelistProvider: WhitelistProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityWhitelistBinding.inflate(layoutInflater)
        VM = ViewModelProvider(this).get(WhitelistViewModel::class.java)
        whitelistProvider = WhitelistProvider.with(this)

        setContentView(B.root)

        attachToolbar()

        VM.liveData.observe(this, {
            updateController(it)
        })
    }

    override fun onDestroy() {
        whitelistProvider.save(VM.selected)
        super.onDestroy()
    }

    private fun attachToolbar() {
        setSupportActionBar(B.layoutToolbar.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f
            actionBar.setTitle(R.string.title_whitelist)
        }
    }

    private fun updateController(blackList: List<App>?) {
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
                        AppViewModel_()
                            .id(it.packageName.hashCode())
                            .app(it)
                            .markChecked(VM.selected.contains(it.packageName))
                            .checked { _, isChecked ->
                                if (isChecked)
                                    VM.selected.add(it.packageName)
                                else
                                    VM.selected.remove(it.packageName)

                                requestModelBuild()
                            }
                    )
                }
            }
        }
    }
}