package com.aurora.services.view.ui.activities

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aurora.services.data.utils.extensions.setLightConfiguration
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.Modifier

abstract class BaseActivity : AppCompatActivity() {

    protected val gson: Gson = GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
            .create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentNightMode =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            setLightConfiguration()
        }
    }
}