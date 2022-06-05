/*****************************************************************************************
 * Copyright <2022> <Saurav Rao> <sauravrao637@gmail.com>                                *
 * Copyright <2022> <Piyush Sharma> <piyushlp@gmail.com>                                 *
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
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.camo.ip_project.R
import com.camo.ip_project.databinding.ActivityTutorialBinding
import com.camo.ip_project.ui.Utility.PERMISSIONS_ALL
import com.camo.ip_project.ui.Utility.PreferenceKey.LAUNCH_COUNT
import com.camo.ip_project.ui.Utility.REQUIRED_PERMISSIONS
import com.camo.ip_project.ui.adapters.LayoutsRVAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

/**
 * This is tutorial activity. It is shown at first launch and asks for required permissions.
 * If all permissions are granted the app proceeds forward.
 * If allowDebugging is true -> skip tutorial and directly check for permissions
 */

@AndroidEntryPoint
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class TutorialActivity : BaseActivity() {

    private lateinit var binding: ActivityTutorialBinding

    private lateinit var pages: ArrayList<Int>
    private lateinit var viewPagerAdapter: LayoutsRVAdapter
    private lateinit var viewPager2: ViewPager2
    private lateinit var layoutdots: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(
            LayoutInflater.from(this)
        )
        setContentView(binding.root)

        if (allowDebugging() || sharedPreferences.getInt(LAUNCH_COUNT, 1) != 1) {
            askPermAndProceed()
        }
        initUI()
    }

    private fun askPermAndProceed() {
        if (allPermissionsGranted()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, PERMISSIONS_ALL
            )
        }
    }

    private fun initUI() {
        pages = arrayListOf(
            R.layout.getting_started_page1
        )
        if (!allPermissionsGranted()) {
            pages.add(R.layout.ask_permissions_page)
        }
        layoutdots = binding.layoutDots
        viewPager2 = binding.viewpager
        viewPagerAdapter = LayoutsRVAdapter(pages)
        viewPager2.adapter = viewPagerAdapter
        TabLayoutMediator(binding.layoutDots, viewPager2) { tab, position ->
        }.attach()
        if (sharedPreferences.getBoolean(Utility.PreferenceKey.DEBUG, false)) {
            viewPager2.currentItem = pages.size - 1
        }

        val btnPrev: Button = findViewById(R.id.btn_prev)
        btnPrev.setOnClickListener {
            val current = getItem(-1)
            if (current >= 0) {
                viewPager2.currentItem = current
            }
        }
        val btnNext: Button = binding.btnNext
        btnNext.setOnClickListener {
            val current = getItem(+1)
            if (current < pages.size) {
                viewPager2.currentItem = current
            } else {
                askPermAndProceed()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_ALL) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission
                    val showRationale = shouldShowRequestPermissionRationale(permission)
                    if (!showRationale) {
                        // user also CHECKED "never ask again"
                        // you can either enable some fall back,
                        // disable features of your app
                        // or open another dialog explaining
                        // again the permission and directing to
                        // the app setting
                        Toast.makeText(this, "Grant Permissions", Toast.LENGTH_LONG).show()
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri: Uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                        finish()
                    }
                }
            }
            if (allPermissionsGranted()) {
                askPermAndProceed()
            }
        }
    }

    private fun getItem(position: Int): Int {
        return viewPager2.currentItem + position
    }
}