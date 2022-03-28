package com.camo.ip_project.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camo.ip_project.ui.Utility.DEFAULT_CST
import com.camo.ip_project.util.Constants.COMPLETE_SAMPLING_PERIOD
import com.camo.ip_project.util.Constants.MIN_RED_INTENSITY
import com.camo.ip_project.util.Utility.toByteArray
import timber.log.Timber
import java.lang.Double.min
import java.util.concurrent.atomic.AtomicBoolean

typealias EndListener = () -> Unit
typealias ProgressListener = (progress: Double) -> Unit
typealias ErrorListener = (error: String) -> Unit
typealias SignalListener = (redIntensity: Double, time: Long) -> Unit

class RAnalyzer(
    private val progressListener: ProgressListener,
    private val endListener: EndListener,
    private val errorListener: ErrorListener,
    private val signalListener: SignalListener,
    private val cameraStabilizingTime: Int = DEFAULT_CST
) :
    ImageAnalysis.Analyzer {

    private val processing = AtomicBoolean(false)
    private var startedAt: Long = -1
    private var stableStartAt: Long = -1
    private var counter = 0

    init {
        Timber.d("RAnalyzer Initialized, cst: $cameraStabilizingTime")
    }
    override fun analyze(image: ImageProxy) {
        if (!processing.compareAndSet(false, true)) {
            image.close()
            return
        }
        val currentTime = System.currentTimeMillis()
        if (startedAt == -1L) {
            progressListener(1.0)
            startedAt = currentTime
            image.close()
            processing.set(false)
            return
        } else {
            if (currentTime - startedAt < cameraStabilizingTime) {
                image.close()
                processing.set(false)
                return
            }
            if (stableStartAt == -1L) stableStartAt = currentTime
        }
        val width = image.width
        val height = image.height
        val data = Yuv.toBuffer(image).buffer.toByteArray()
        image.close()
        counter++

        val imgRgbAvgList = ImageProcessing.decodeYUV420SPtoRGBAvg(data, width, height)
        val imgRAvg = imgRgbAvgList[0]
        Timber.d("counter $counter redAvg $imgRAvg")

        if (imgRAvg < MIN_RED_INTENSITY) {
            errorListener("Bad Frame")
            reset()
            return
        }
        signalListener(imgRAvg, currentTime)
        val elapsedAnalysisTime = (currentTime - stableStartAt) / 1000
        progressListener(min(100.0, elapsedAnalysisTime * 100.0 / COMPLETE_SAMPLING_PERIOD))

        if (elapsedAnalysisTime >= COMPLETE_SAMPLING_PERIOD) {
            endListener()
            reset()
            return
        }
        processing.set(false)
    }

    private fun reset() {
        processing.set(false)
        startedAt = -1
        stableStartAt = -1
    }
}