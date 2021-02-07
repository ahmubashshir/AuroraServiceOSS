package com.aurora.services.view.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.aurora.services.R
import com.aurora.services.data.utils.extensions.isPermissionGranted
import com.aurora.services.data.utils.extensions.isSystemApp
import com.aurora.services.data.utils.extensions.open
import com.aurora.services.databinding.ActivityAuroraBinding

class AuroraActivity : BaseActivity() {

    private lateinit var B: ActivityAuroraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        B = ActivityAuroraBinding.inflate(layoutInflater)
        setContentView(B.root)
        inflateStatus()
    }

    private fun inflateStatus() {
        with(B.layoutStatus) {
            if (isSystemApp()) {
                txtStatus.text = getString(R.string.service_enabled)
                txtStatus.setTextColor(resources.getColor(R.color.colorGreen))
            } else {
                txtStatus.text = getString(R.string.service_disabled)
                txtStatus.setTextColor(resources.getColor(R.color.colorRed))
            }

            if (isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                txtPermission.text = getString(R.string.perm_granted)
                txtPermission.setTextColor(resources.getColor(R.color.colorGreen))
            } else {
                txtPermission.text = getString(R.string.perm_not_granted)
                txtPermission.setTextColor(resources.getColor(R.color.colorRed))
                askPermissions()
            }

            cardWhitelist.setOnClickListener {
                open(WhitelistActivity::class.java)
            }

            cardLog.setOnClickListener {
                open(StatisticsActivity::class.java)
            }
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