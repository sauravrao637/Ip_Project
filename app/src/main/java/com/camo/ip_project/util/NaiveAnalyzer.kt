package com.camo.ip_project.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camo.ip_project.util.Constants.COMPLETE_SAMPLING_PERIOD
import com.camo.ip_project.util.Constants.ESTIMATION_SAMPLING_PERIOD
import com.camo.ip_project.util.Constants.MAX_HR_RANGE
import com.camo.ip_project.util.Constants.MIN_HR_RANGE
import com.camo.ip_project.util.Constants.MIN_RED_INTENSITY
import com.camo.ip_project.util.ImageProcessing.decodeYUV420SPtoRGBAvg
import com.camo.ip_project.util.Utility.toByteArray
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

typealias HeartBeatListener = (beat: Beat) -> Unit

class NaiveAnalyzer(private val listener: HeartBeatListener) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        if (!processing.compareAndSet(false, true)) {
            image.close()
            return
        }
//        ignore first few frames
        if (justStarted && System.currentTimeMillis() - startTime < 1000) {
            processing.set(false)
            image.close()
            return
        } else if (justStarted) {
            justStarted = false
            startTime = System.currentTimeMillis()
        }
        val width = image.width
        val height = image.height
        val data = Yuv.toBuffer(image).buffer.toByteArray().clone()
        val imgAvg = decodeYUV420SPtoRGBAvg(
            data,
            width,
            height
        )[0]

        Timber.d("imgAvg: $imgAvg, frameCounter = $counter")

        if ((imgAvg < MIN_RED_INTENSITY)) {
            processing.set(false)
            image.close()
            listener(Beat.error("Bad Frames"))
            Timber.d("Bad Frame no. = $counter")
            return
        }
        counter++
        var averageArrayAvg = 0.0
        var averageArrayCnt = 0.0
        for (i in averageArray.indices) {
            if (averageArray[i] > 0) {
                averageArrayAvg += averageArray[i]
                averageArrayCnt++
            }
        }
        val rollingAverage = if (averageArrayCnt > 0) averageArrayAvg / averageArrayCnt else 0.0
        var newType = current
        if (imgAvg < rollingAverage) {
            newType = TYPE.RED
            if (newType != current) {
                beats++
                listener(Beat.beat())
                if (prevRPeakTime != -1L) {
                    val intervalSquare = (1.0 * (System.currentTimeMillis() - prevRPeakTime)).pow(2)
                    sum += intervalSquare
                }
                prevRPeakTime = System.currentTimeMillis()
                total++
                Timber.d("BEAT!! beats=$beats")
            }
        } else if (imgAvg > rollingAverage) {
            newType = TYPE.GREEN
        }
        if (averageIndex == RED_AVG_ARRAY_SIZE) averageIndex = 0
        averageArray[averageIndex] = imgAvg
        averageIndex++

        // Transitioned from one state to another to the same
        if (newType != current) {
            current = newType
        }
        val endTime = System.currentTimeMillis()
        val totalTimeInSecs = (endTime - startTime) / 1000.0
        Timber.d("totalTime: $totalTimeInSecs s")
        if (totalTimeInSecs >= ESTIMATION_SAMPLING_PERIOD) {
            val bps = beats / totalTimeInSecs
            val bpm = (bps * 60.0).toInt()

            Timber.d("bpm:$bpm")
            if (bpm < MIN_HR_RANGE || bpm > MAX_HR_RANGE) {
                startTime = System.currentTimeMillis()
                beats = 0.0
                processing.set(false)
                image.close()
                return
            }
            if (beatsIndex == BEATS_ARRAY_SIZE) beatsIndex = 0
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

            val bpmAvg = if (beatsArrayCnt != 0) beatsArrayAvg / beatsArrayCnt else 0
            Timber.d("bpm:$bpmAvg")
            val rmssd = (sum / total).pow(0.5) / bpm
            Timber.d("RMSSD: $rmssd, sum: $sum, total: $total")
            if (beatsIndex < beatsArray.size)
                listener(Beat.poorEstimateData(Beat.BeatData(bpmAvg, rmssd)))
            else listener(Beat.finalBeatData(Beat.BeatData(bpmAvg, rmssd)))
            startTime = System.currentTimeMillis()
            beats = 0.0
            resetRmssd()
        }
        listener(Beat.progress((totalTimeInSecs * 10).toInt()))
        processing.set(false)
        image.close()
    }

    private val processing = AtomicBoolean(false)
    private var averageIndex = 0

    private val averageArray = DoubleArray(RED_AVG_ARRAY_SIZE)
    private var current = TYPE.GREEN
    private var beatsIndex = 0

    private val beatsArray = IntArray(BEATS_ARRAY_SIZE)
    private var beats = 0.0
    private var startTime: Long = System.currentTimeMillis()
    private var counter = 0
    private var justStarted = true

    //    RMSSD
    private var prevRPeakTime: Long = -1
    private var sum = 0.0
    private var total = 0

    fun resetRmssd() {
        sum = 0.0
        prevRPeakTime = -1
        total = 0
    }

    companion object {
        private const val RED_AVG_ARRAY_SIZE = 20
        private const val BEATS_ARRAY_SIZE = COMPLETE_SAMPLING_PERIOD / ESTIMATION_SAMPLING_PERIOD
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
    val data: BeatData?,
    val progress: Int?,
    val error: String?,

    ) {
    class BeatData(val bpm: Int, val rmssd: Double)
    companion object {
        fun analysing(imgAvg: Int, time: Long): Beat =
            Beat(BeatDataType.ANALYSIS, imgAvg, time, null, null, null)

        fun finalBeatData(data: BeatData): Beat =
            Beat(BeatDataType.FINAL, null, null, data, null, null)

        fun progress(progress: Int): Beat =
            Beat(BeatDataType.Progress, null, null, null, progress, null)

        fun beat(): Beat = Beat(BeatDataType.Beat, null, null, null, null, null)

        fun poorEstimateData(data: BeatData): Beat =
            Beat(BeatDataType.Estimated, null, null, data, null, null)

        fun error(error: String?): Beat =
            Beat(BeatDataType.Error, null, null, null, null, error ?: "E")
    }
}