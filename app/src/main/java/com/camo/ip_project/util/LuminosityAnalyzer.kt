package com.camo.ip_project.util

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean


typealias LumaListener = (luma: Double) -> Unit

class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
    val TAG = "LumAnal"
    private val processing: AtomicBoolean = AtomicBoolean(false)
    private var averageIndex = 0
    private val averageArraySize = 4
    private val averageArray = IntArray(averageArraySize)


    private var beatsIndex = 0
    private val beatsArraySize = 3
    private val beatsArray = IntArray(beatsArraySize)
    private var beats = 0.0
    private var startTime: Long = 0

    private var currentType = TYPE.GREEN

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    override fun analyze(image: ImageProxy) {
        if (!processing.compareAndSet(false, true)) return;
        val width: Int = image.width
        val height: Int = image.height
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data,width/2,height/2)
        Timber.d("imgavg: $imgAvg")
        printt()
        if (imgAvg == 0 || imgAvg == 255) {
            processing.set(false);
            image.close()
            return;
        }
        var averageArrayAvg = 0
        var averageArrayCnt = 0

        for (i in averageArray.indices) {
            if (averageArray[i] > 0) {
                averageArrayAvg += averageArray[i]
                averageArrayCnt++
            }
        }
        val rollingAverage = if (averageArrayCnt > 0) averageArrayAvg / averageArrayCnt else 0
        var newType: TYPE = currentType
        if (imgAvg < rollingAverage) {
            newType = TYPE.RED
            if (newType !== currentType) {
                beats++
                // Log.d(TAG, "BEAT!! beats="+beats);
            }
        } else if (imgAvg > rollingAverage) {
            newType = TYPE.GREEN
        }

        if (averageIndex == averageArraySize) averageIndex = 0
        averageArray[averageIndex] = imgAvg
        averageIndex++

        // Transitioned from one state to another to the same

        // Transitioned from one state to another to the same
        if (newType !== currentType) {
            currentType = newType
        }

        val endTime = System.currentTimeMillis()
        val totalTimeInSecs = (endTime - startTime) / 1000.0
        if (totalTimeInSecs >= 10) {
            val bps = beats / totalTimeInSecs
            val dpm = (bps * 60.0).toInt()
            if (dpm < 30 || dpm > 180) {
                startTime = System.currentTimeMillis()
                beats = 0.0
                processing.set(false)
                image.close()
                return
            }

             Log.d(TAG,
             "totalTimeInSecs="+totalTimeInSecs+" beats="+beats);
            if (beatsIndex == beatsArraySize) beatsIndex = 0
            beatsArray[beatsIndex] = dpm
            beatsIndex++
            var beatsArrayAvg = 0
            var beatsArrayCnt = 0
            for (i in beatsArray.indices) {
                if (beatsArray[i] > 0) {
                    beatsArrayAvg += beatsArray[i]
                    beatsArrayCnt++
                }
            }
            val beatsAvg = beatsArrayAvg / beatsArrayCnt
            listener(beatsAvg*1.0)
            startTime = System.currentTimeMillis()
            beats = 0.0
        }
        processing.set(false)
        image.close()
        return
    }

    private fun printt() {
        Timber.d("${currentType},$averageArray,$beatsArray, $startTime")
    }
}
enum class TYPE {
    GREEN, RED
}
