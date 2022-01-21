package com.camo.ip_project.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camo.ip_project.util.Constants.MAX_HR_RANGE
import com.camo.ip_project.util.Constants.MIN_HR_RANGE
import com.camo.ip_project.util.Constants.MIN_RED_INTENSITY
import com.camo.ip_project.util.Constants.SAMPLING_PERIOD
import com.camo.ip_project.util.Utility.toByteArray
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean


typealias LumaListener = (beat: Beat) -> Unit

class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
    private val processing: AtomicBoolean = AtomicBoolean(false)
    private var averageIndex = 0
    private val averageArraySize = 4
    private val averageArray = IntArray(averageArraySize)


    private var beatsIndex = 0
    private val beatsArraySize = 3
    private val beatsArray = IntArray(beatsArraySize)
    private var beats = 0
    private var startTime: Long = -1
    private var processingStartTime = System.currentTimeMillis()
    private var currentType = TYPE.GREEN

    init {
        Timber.d("Luminosity Analyzer")
    }

    override fun analyze(image: ImageProxy) {
        if (!processing.compareAndSet(false, true)) {
            image.close()
            return
        }
        if(startTime == -1L) startTime = System.currentTimeMillis()
        val width = image.width
        val height = image.height
        val data = Yuv.toBuffer(image).buffer.toByteArray()
        val imgAvg =
            ImageProcessing.decodeYUV420SPtoRGBAvg(data, width, height)[0].toInt()

        printt(imgAvg)

        if (imgAvg < MIN_RED_INTENSITY) {
            listener(Beat.error("Place Finger properly"))
            processing.set(false)
            image.close()
            return
        }
        var averageArrayAvg = 0
        var averageArrayCnt = 0

        for (i in averageArray.indices) {
            if (averageArray[i] > MIN_RED_INTENSITY) {
                averageArrayAvg += averageArray[i]
                averageArrayCnt++
            }
        }
        val rollingAverage = if (averageArrayCnt > 0) (averageArrayAvg / averageArrayCnt) else 0
        var newType: TYPE = currentType
        if (imgAvg < rollingAverage) {
            newType = TYPE.RED
            if (newType != currentType) {
                beats++
                listener(Beat.beat())
                Timber.d("BEAT!! beats=$beats");
            }
        } else if (imgAvg > rollingAverage) {
            newType = TYPE.GREEN
        }

        if (averageIndex == averageArraySize) averageIndex = 0
        averageArray[averageIndex] = imgAvg
        averageIndex++

        // Transitioned from one state to another to the same

        currentType = newType

        val totalTimeInSecs = (System.currentTimeMillis() - startTime) / 1000.0
        val bpm = ((beats / totalTimeInSecs) * 60.0).toInt()

        Timber.d("totalTimeInSecs= $totalTimeInSecs , beats=$beats")

        if (totalTimeInSecs >= SAMPLING_PERIOD) {
            if (bpm < MIN_HR_RANGE || bpm > MAX_HR_RANGE) {
                listener(Beat.error("Error $bpm"))
                beats = 0
                startTime = System.currentTimeMillis()
                processing.set(false)
                image.close()
                return
            }

            if (beatsIndex == beatsArraySize) beatsIndex = 0
            beatsArray[beatsIndex] = bpm
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
            listener(Beat.finalBeats(beatsAvg))
            startTime = System.currentTimeMillis()
            beats = 0
        }
//        if (totalTimeInSecs >= ESTIMATION_PERIOD) listener(Beat.poorEstimationBeats(bpm))
        listener(Beat.progress((totalTimeInSecs*100/ SAMPLING_PERIOD).toInt()))
        processing.set(false)
        image.close()
        return
    }

    private fun printt(imgAvg: Int) {
        Timber.d("imgAvg: $imgAvg, ${currentType},${averageArray.contentToString()},${beatsArray.contentToString()}")
    }
}

enum class TYPE {
    GREEN, RED
}

enum class BeatDataType {
    ANALYSIS, FINAL, Progress, Beat, Estimated, Error
}

data class Beat(
    val beatDataType: BeatDataType,
    val imgAvg: Int?,
    val time: Long?,
    val beats: Int?,
    val progress: Int?,
    val error: String?
) {
    companion object {
        fun analysing(imgAvg: Int, time: Long): Beat =
            Beat(BeatDataType.ANALYSIS, imgAvg, time, null, null, null)

        fun finalBeats(beats: Int): Beat = Beat(BeatDataType.FINAL, null, null, beats, null, null)

        fun progress(progress: Int): Beat =
            Beat(BeatDataType.Progress, null, null, null, progress, null)

        fun beat(): Beat = Beat(BeatDataType.Beat, null, null, null, null, null)

        fun poorEstimationBeats(beats: Int): Beat =
            Beat(BeatDataType.Estimated, null, null, beats, null, null)

        fun error(error: String?): Beat =
            Beat(BeatDataType.Error, null, null, null, null, error ?: "E")
    }
}
