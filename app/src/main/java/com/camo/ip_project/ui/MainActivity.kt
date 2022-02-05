package com.camo.ip_project.ui

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.camo.ip_project.R
import com.camo.ip_project.databinding.ActivityMainBinding
import com.camo.ip_project.ui.Utility.PERMISSIONS_ALL
import com.camo.ip_project.ui.Utility.PreferenceKey.CAMERA_STABILIZING_TIME
import com.camo.ip_project.ui.Utility.PreferenceKey.DBG_CATEGORY
import com.camo.ip_project.ui.Utility.PreferenceKey.LAUNCH_COUNT
import com.camo.ip_project.ui.Utility.REQUIRED_PERMISSIONS
import com.camo.ip_project.ui.adapters.GettingStartedVpAdapter
import com.camo.ip_project.util.Constants.DEBUG_MODE
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var pages: ArrayList<Int>
    private lateinit var viewPagerAdapter: GettingStartedVpAdapter
    private lateinit var viewPager2: ViewPager2
    private lateinit var layoutdots: TabLayout

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(
            LayoutInflater.from(this)
        )
        setContentView(binding.root)

        setDefaultPreferences()

        if(DEBUG_MODE || sharedPreferences.getInt(LAUNCH_COUNT,1)==1){
            askPermAndProceed()
        }
        initUI()
    }

    private fun setDefaultPreferences() {
        val editor = sharedPreferences.edit()
        if(!sharedPreferences.contains(Utility.PreferenceKey.DEBUG)){
            editor.putBoolean(Utility.PreferenceKey.DEBUG,false)
            editor.putString(CAMERA_STABILIZING_TIME,"2")
        }
        editor.putInt(LAUNCH_COUNT,sharedPreferences.getInt(LAUNCH_COUNT,0)+1)
        editor.apply()
    }

    private fun askPermAndProceed() {
        if(allPermissionsGranted()){
            val intent = Intent(this, MainActivity2::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }else{
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, PERMISSIONS_ALL
            )
        }
    }

    private fun initUI() {
        pages = arrayListOf(
            R.layout.getting_started_page1
        )
        if(!allPermissionsGranted()){
            pages.add(R.layout.ask_permissions_page)
        }
        layoutdots = binding.layoutDots
        viewPager2 = binding.viewpager
        viewPagerAdapter = GettingStartedVpAdapter(this, pages)
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
                val permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission
                    val showRationale = shouldShowRequestPermissionRationale(permission);
                    if (! showRationale) {
                        // user also CHECKED "never ask again"
                        // you can either enable some fall back,
                        // disable features of your app
                        // or open another dialog explaining
                        // again the permission and directing to
                        // the app setting
                        Toast.makeText(this,"Yaarr...", Toast.LENGTH_LONG).show()
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