/*****************************************************************************************
 * Copyright <2022> <Saurav Rao> <sauravrao637@gmail.com>                                *
 *                                                                                       *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this  *
 * software and associated documentation files (the "Software"), to deal in the Software *
 * without restriction, including without limitation the rights to use, copy, modify,    *
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to    *
 * permit persons to whom the Software is furnished to do so, subject to the following   *
 * conditions:                                                                           *
 *                                                                                       *
 * The above copyright notice and this permission notice shall be included in all copies *
 * or substantial portions of the Software.                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,   *
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A         *
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT    *
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF  *
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE  *
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                                         *
 *****************************************************************************************/

package com.camo.ip_project.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.camo.ip_project.databinding.StartActivityBinding
import com.camo.ip_project.ui.Utility.DEFAULT_CST
import com.camo.ip_project.ui.Utility.SPLASH_SCREEN_TIME
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * This is responsible for starting the app with splash screen and set up preferences to default if
 * not already set (as in case of first launch)
 *
 * Skips delay on splash screen when debugging is true
 */
@AndroidEntryPoint
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class StartActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = StartActivityBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setDefaultPreferences()

        lifecycleScope.launchWhenStarted {
            if (!allowDebugging()) delay(SPLASH_SCREEN_TIME)
            withContext(Dispatchers.Main) {
                val intent = Intent(this@StartActivity, TutorialActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setDefaultPreferences() {
        val editor = sharedPreferences.edit()
        //if preferences not already set
        if (!sharedPreferences.contains(Utility.PreferenceKey.DEBUG)) {
            editor.putBoolean(Utility.PreferenceKey.DEBUG, false)
            editor.putString(Utility.PreferenceKey.CAMERA_STABILIZING_TIME, DEFAULT_CST.toString())
            editor.putInt(Utility.PreferenceKey.LAUNCH_COUNT, 1)
            editor.apply()
            return
        }
        //update the required preferences if already set
        editor.putInt(
            Utility.PreferenceKey.LAUNCH_COUNT,
            sharedPreferences.getInt(Utility.PreferenceKey.LAUNCH_COUNT, 0) + 1
        )
        editor.apply()
    }
}