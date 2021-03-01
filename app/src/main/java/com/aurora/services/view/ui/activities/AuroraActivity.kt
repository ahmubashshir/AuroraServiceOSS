package com.aurora.services.view.ui.activities

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MenuItem
import com.aurora.services.BuildConfig
import com.aurora.services.R
import com.aurora.services.data.model.DashboardItem
import com.aurora.services.data.utils.extensions.isPermissionGranted
import com.aurora.services.data.utils.extensions.isSystemApp
import com.aurora.services.data.utils.extensions.open
import com.aurora.services.databinding.ActivityAuroraBinding
import com.aurora.services.view.epoxy.DashboardViewModel_
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions

class AuroraActivity : BaseActivity() {

    private lateinit var B: ActivityAuroraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityAuroraBinding.inflate(layoutInflater)
        setContentView(B.root)
        attachToolbar()
        inflateStatus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
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
            actionBar.title = getString(R.string.app_name)
            actionBar.subtitle = BuildConfig.VERSION_NAME
        }
    }

    private fun inflateStatus() {
        val dashboardItemBoardItems: List<DashboardItem> = listOf(
            DashboardItem(
                id = 0,
                title = getString(R.string.title_service),
                subtitle = if (isSystemApp())
                    getString(R.string.service_enabled)
                else
                    getString(R.string.service_disabled),
                icon = R.drawable.ic_service
            ),
            DashboardItem(
                id = 1,
                title = getString(R.string.title_permission),
                subtitle = if (isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    getString(R.string.perm_granted)
                else
                    getString(R.string.perm_not_granted),
                icon = R.drawable.ic_permission
            ),
            DashboardItem(
                id = 2,
                title = getString(R.string.title_statistics),
                subtitle = getString(R.string.title_statistics_desc),
                icon = R.drawable.ic_log
            )
        )

        B.epoxyRecycler.withModels {
            dashboardItemBoardItems.forEach {
                add(
                    DashboardViewModel_()
                        .id(it.id)
                        .dash(it)
                        .click { _ ->
                            when (it.id) {
                                1 -> {
                                    checkStorageAccessPermission()
                                    checkStorageManagerPermission()
                                }
                                2 -> {
                                    open(StatisticsActivity::class.java)
                                }
                            }
                        }
                )
            }
        }
    }

    private fun checkStorageAccessPermission() = runWithPermissions(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) {
        B.epoxyRecycler.requestModelBuild()
    }

    private fun checkStorageManagerPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                    99
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            99 -> {
                inflateStatus()
            }
        }
    }
}