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
import com.camo.ip_project.R
import java.io.File

/**
 * This is utility class for ui/context related functionalities
 */
object Utility {
    /**
     * time period in milliseconds for splash screen is shown
     */
    const val SPLASH_SCREEN_TIME = 1000L
    val REQUIRED_PERMISSIONS =
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    const val PERMISSIONS_ALL = 1

    /**
     * DEFAULT_CST is the default camera stabilizing time in milliseconds
     */
    const val DEFAULT_CST = 1000

    private const val OUTPUT_DATA_FILE_NAME_PREFIX = "hrv_processing_data"
    fun getFileNameForHrvData(username: String, timestamp: Long): String {
        return "${OUTPUT_DATA_FILE_NAME_PREFIX}_${timestamp}_$username"
    }

    /**
     * This class stores the keys used in shared preferences.
     */
    object PreferenceKey {
        const val DEBUG = "debug"

        /**
         * LAUNCH_COUNT stores the app launch counter. It increases every time the app is started.
         */
        const val LAUNCH_COUNT = "launch_count"
        const val DBG_CATEGORY = "dbg_category"

        /**
         * CST is camera stabilizing time. When the camera is just opened, it shows black frames for
         * some small time period. We need to ignore these frames. CST is this time period in
         * milliseconds.
         */
        const val CAMERA_STABILIZING_TIME = "dbg_cam_stabilizing_time"
    }

    fun getOutputDirectory(context: Context): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir
    }
}