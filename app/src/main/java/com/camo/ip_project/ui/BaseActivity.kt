package com.camo.ip_project.ui

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.camo.ip_project.ui.Utility.DEFAULT_CST
import com.camo.ip_project.ui.Utility.PreferenceKey.CAMERA_STABILIZING_TIME
import com.camo.ip_project.ui.Utility.PreferenceKey.DEBUG
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    fun isDebugging() = sharedPreferences.getBoolean(DEBUG, false)

    fun getPreferredCST(): Int =
        if (isDebugging()) sharedPreferences.getString(CAMERA_STABILIZING_TIME, "DEFAULT")?.toInt()
            ?.times(1000) ?: DEFAULT_CST else DEFAULT_CST
}