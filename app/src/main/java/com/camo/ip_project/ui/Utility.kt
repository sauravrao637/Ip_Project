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

import android.Manifest
import android.content.Context
import android.widget.Toast
import com.camo.ip_project.R
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException

object Utility {
    val REQUIRED_PERMISSIONS =
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    const val PERMISSIONS_ALL = 1
    const val DEFAULT_CST = 1000
    val OUTPUT_DATA: String get() = run { "hrv_processing_data_${System.currentTimeMillis()}.txt" }

    object PreferenceKey {
        const val DEBUG = "debug"
        const val LAUNCH_COUNT = "launch_count"
        const val DBG_CATEGORY = "dbg_category"
        const val CAMERA_STABILIZING_TIME = "dbg_cam_stabilizing_time"
    }

    fun getOutputDirectory(context: Context): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir
    }

    fun saveHrvData(
        context: Context,
        redAvgList: ArrayList<Double>,
        timestamps: ArrayList<Double>
    ) {
        try {
            val out = FileWriter(
                File(
                    getOutputDirectory(context),
                    "hrv_processing_data_${System.currentTimeMillis()}.txt"
                )
            )
            val c = "RedAvgList\n ${redAvgList.toList()} \n TimeStamps \n${timestamps.toList()}"
            out.write(c)
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Could not save output file", Toast.LENGTH_LONG).show()
            Timber.e(e)
        }
    }
}