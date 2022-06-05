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
import timber.log.Timber
import javax.inject.Inject

/**
 * This fragment is responsible for managing different settings across the application
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    init {
        Timber.i("initialized")
    }

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