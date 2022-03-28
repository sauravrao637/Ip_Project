package com.camo.ip_project.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.camo.ip_project.databinding.StartActivityBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@AndroidEntryPoint
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class StartActivity : BaseActivity() {
    private lateinit var binding: StartActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StartActivityBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setDefaultPreferences()
        lifecycleScope.launchWhenStarted {
            if(!isDebugging())delay(1000)
            withContext(Dispatchers.Main){
                val intent = Intent(this@StartActivity, TutorialActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        }
    }
    private fun setDefaultPreferences() {
        val editor = sharedPreferences.edit()
        if(!sharedPreferences.contains(Utility.PreferenceKey.DEBUG)){
            editor.putBoolean(Utility.PreferenceKey.DEBUG,false)
            editor.putString(Utility.PreferenceKey.CAMERA_STABILIZING_TIME,"2")
        }
        editor.putInt(Utility.PreferenceKey.LAUNCH_COUNT,sharedPreferences.getInt(Utility.PreferenceKey.LAUNCH_COUNT,0)+1)
        editor.apply()
    }
}