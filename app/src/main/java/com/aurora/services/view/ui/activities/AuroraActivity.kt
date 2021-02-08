package com.aurora.services.view.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.aurora.services.R
import com.aurora.services.data.model.Dash
import com.aurora.services.data.utils.extensions.isPermissionGranted
import com.aurora.services.data.utils.extensions.isSystemApp
import com.aurora.services.data.utils.extensions.open
import com.aurora.services.databinding.ActivityAuroraBinding
import com.aurora.services.view.epoxy.DashboardViewModel_

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
            actionBar.setTitle(R.string.app_name)
        }
    }

    private fun inflateStatus() {
        B.epoxyRecycler.withModels {
            add(
                DashboardViewModel_()
                    .id(0)
                    .dash(
                        Dash(
                            id = 0,
                            title = getString(R.string.title_service),
                            subtitle = if (isSystemApp())
                                getString(R.string.service_enabled)
                            else
                                getString(R.string.service_enabled),
                            icon = R.drawable.ic_service
                        )
                    )
            )
            add(
                DashboardViewModel_()
                    .id(0)
                    .dash(
                        Dash(
                            id = 1,
                            title = getString(R.string.title_permission),
                            subtitle = if (isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                                getString(R.string.perm_granted)
                            else
                                getString(R.string.perm_not_granted),
                            icon = R.drawable.ic_permission
                        )
                    ).click { _ -> askPermissions() }
            )
            add(
                DashboardViewModel_()
                    .id(0)
                    .dash(
                        Dash(
                            id = 2,
                            title = getString(R.string.title_statistics),
                            subtitle = getString(R.string.title_statistics_desc),
                            icon = R.drawable.ic_log
                        )
                    )
                    .click { _ -> open(StatisticsActivity::class.java) }
            )
        }
    }

    private fun askPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1337
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1337 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    inflateStatus()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}