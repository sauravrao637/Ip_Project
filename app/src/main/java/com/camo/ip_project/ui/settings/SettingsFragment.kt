package com.camo.ip_project.ui.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.camo.ip_project.R
import com.camo.ip_project.ui.Utility.PreferenceKey.DBG_CATEGORY
import com.camo.ip_project.ui.Utility.PreferenceKey.DEBUG
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    private var listener: OnSharedPreferenceChangeListener? = null
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        val value = sharedPreferences.getBoolean(DEBUG, false)
        (findPreference(DBG_CATEGORY) as PreferenceCategory?)?.isVisible = value
        listener = OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                DEBUG -> {
                    val valuee = prefs.getBoolean(DEBUG, false)
                    (findPreference(DBG_CATEGORY) as PreferenceCategory?)?.isVisible = valuee
                }
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }
}