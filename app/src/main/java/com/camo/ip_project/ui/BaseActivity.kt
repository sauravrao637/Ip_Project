/****************************************************************************************
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
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,   *
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A         *
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT    *
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF  *
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE  *
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                                         *
 *****************************************************************************************/

package com.camo.ip_project.ui

import android.content.SharedPreferences
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.camo.ip_project.ui.Utility.DEFAULT_CST
import com.camo.ip_project.ui.Utility.PreferenceKey.CAMERA_STABILIZING_TIME
import com.camo.ip_project.ui.Utility.PreferenceKey.DEBUG
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject

/*
Every activity of this app must be child of this activity.
 */
@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    fun allowDebugging() = sharedPreferences.getBoolean(DEBUG, false)

    fun getPreferredCST(): Int =
        if (allowDebugging()) {
            when (val cst =
                sharedPreferences.getString(CAMERA_STABILIZING_TIME, "DEFAULT")?.toInt()) {
                null, 0 -> {
                    DEFAULT_CST
                }
                else -> cst
            }
        } else DEFAULT_CST

    fun writeToTxtFile(fileName: String, text: String) {
        try {
            val out = FileWriter(File(Utility.getOutputDirectory(applicationContext), fileName))
            out.apply {
                write(text)
                close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Could not save file", Toast.LENGTH_SHORT).show()
            Timber.e(e)
        }
    }
}