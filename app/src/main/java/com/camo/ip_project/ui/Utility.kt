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
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    const val PERMISSIONS_ALL = 1
    object PreferenceKey{
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

    fun saveHrvData(context: Context,redAvgList: ArrayList<Double>, timestamps: ArrayList<Double>) {
        try {
            val out = FileWriter(File(getOutputDirectory(context), "hrv_processing_data.txt"))
            val c = redAvgList.toList().toString() +"\n"+ timestamps.toList().toString()
            out.write(c)
            out.close()
        } catch (e: IOException) {
            Toast.makeText(context,"Could not save output file", Toast.LENGTH_LONG).show()
            Timber.e(e)
        }
    }
}