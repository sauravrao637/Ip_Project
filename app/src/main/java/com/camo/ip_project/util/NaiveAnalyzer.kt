package com.camo.ip_project.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camo.ip_project.util.Constants.COMPLETE_SAMPLING_PERIOD
import com.camo.ip_project.util.Constants.ESTIMATION_SAMPLING_PERIOD
import com.camo.ip_project.util.Constants.MAX_HR_RANGE
import com.camo.ip_project.util.Constants.MIN_HR_RANGE
import com.camo.ip_project.util.Constants.MIN_RED_INTENSITY
import com.camo.ip_project.util.ImageProcessing.decodeYUV420SPtoRGBAvg
import com.camo.ip_project.util.ImageProcessing.fft
import com.camo.ip_project.util.ImageProcessing.fft2
import com.camo.ip_project.util.Utility.getVarianceDouble
import com.camo.ip_project.util.Utility.getVarianceLong
import com.camo.ip_project.util.Utility.toByteArray
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

typealias HeartBeatListener = (beat: Beat) -> Unit

class NaiveAnalyzer(private val listener: HeartBeatListener) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        if (!processing.compareAndSet(false, true)) {
            image.close()
            return
        }
        val currentTime = System.currentTimeMillis()
//        ignore first few frames
        if (justStarted && currentTime - startTime < 1000) {
            processing.set(false)
            image.close()
            return
        } else if (justStarted) {
            justStarted = false
            startTime = currentTime
        }
        val width = image.width
        val height = image.height
        val data = Yuv.toBuffer(image).buffer.toByteArray()
        image.close()
        val imgRgbAvgList = decodeYUV420SPtoRGBAvg(data, width, height)
        var imgRAvg = imgRgbAvgList[0]
        var imgBAvg = imgRgbAvgList[2]
        Timber.d("imgAvg: $imgRAvg, frameCounter = $analysed_frames, currentTime: $currentTime")

//        report if frame is not good enough
        if ((imgRAvg < MIN_RED_INTENSITY)) {
            processing.set(false)
            listener(Beat.error("Bad Frames"))
            Timber.d("Bad Frame no. = $analysed_frames")
            analysed_frames = 0
            startTime = currentTime
            rrIntervalsList.clear()
            imgBAvgArray.clear()
            imgBAvgArray.clear()
            return
        }
        if(rAvgList.size>=1){
            svR += (imgRAvg - svR)/4
            imgRAvg = svR

            svB += (imgBAvg - svB)/4
            imgBAvg = svB
        }else{
            svR = imgRAvg
            svB = imgBAvg
        }
        rAvgList.add(imgRAvg)
        timeList.add(currentTime)

        analysed_frames++

        imgRAvgArray.add(imgRAvg)
        imgBAvgArray.add(imgBAvg)

        val rollingRAverage = if (averageArray.isNotEmpty()) averageArray.sum() / averageArray.size else 0.0

        var newType = current
        if (imgRAvg < rollingRAverage) {
            newType = TYPE.RED
            if (newType != current) {
                beats++
                Timber.d("BEAT!! beats=$beats")
                listener(Beat.beat())

//                calculate RR interval in ms
                if (prevRPeakTime != -1L) {
                    val currInterval = currentTime - prevRPeakTime
                    rrIntervalsList.add(currInterval)
                    prevInterval = currInterval
                }
                prevRPeakTime = currentTime
            }
        } else if (imgRAvg > rollingRAverage) {
            newType = TYPE.GREEN
        }

        if (averageIndex == RED_AVG_ARRAY_SIZE) averageIndex = 0

        averageArray[averageIndex] = imgRAvg
        averageIndex++

        // Transitioned from one state to another to the same
        current = newType

        val totalTime = (currentTime - startTime) / 1000.0

        if (totalTime >= ESTIMATION_SAMPLING_PERIOD) {
            Timber.d("SAMPLE | Start time: ${startTime / 1000}, current time: ${currentTime/1000}, total time: $totalTime")

            val red: Array<Double> = imgRAvgArray.toArray(arrayOfNulls(imgRAvgArray.size))
            val samplingFreq = imgRAvgArray.size / totalTime
            val bpmFft = (60 * fft(red, red.size, samplingFreq)).toInt()
            val breathFFt = (60 * fft2(red, red.size, samplingFreq)).toInt()

            val meanR = imgRAvgArray.sum() / imgRAvgArray.size
            val meanB = imgBAvgArray.sum() / imgBAvgArray.size
            val varR = getVarianceDouble(imgRAvgArray)
            val varB = getVarianceDouble(imgBAvgArray)
            val ratio = (varR / meanR) / (varB / meanB)
            val spo2 = 100 - 5 * ratio

            imgRAvgArray.clear()
            imgBAvgArray.clear()

            Timber.d("SAMPLE | FFT bpm = $bpmFft, FFT breaths = $breathFFt, spo2: $spo2")

            val bps = beats / totalTime
            val bpm = (bps * 60.0).toInt()

            Timber.d("SAMPLE | bpm:$bpm")
            if (bpm < MIN_HR_RANGE || bpm > MAX_HR_RANGE) {
                startTime = currentTime
                beats = 0.0
                processing.set(false)
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
            Timber.d("SAMPLE | bpm avg over array:$bpmAvg")

//            val rmssd = if (total <= 2) 0.0 else ((sum / (total - 2)).pow(0.5))

            val rmssd = getVarianceLong(rrIntervalsList)
            Timber.d("SAMPLE | RMSSD: $rmssd, intervalArray: $rrIntervalsList")
            if (beatsIndex < beatsArray.size)
                listener(Beat.poorEstimateData(Beat.BeatData(bpmAvg, rmssd)))
            else {
                Timber.d("DBG "+rAvgList.toArray().contentToString())
                Timber.d("DBG "+timeList.toArray().contentToString())
                listener(Beat.finalBeatData(Beat.BeatData(bpmAvg, rmssd)))
            }
            startTime = currentTime
            beats = 0.0

        }
        listener(Beat.progress((totalTime * 100 / ESTIMATION_SAMPLING_PERIOD).toInt()))
        processing.set(false)

    }

    private val processing = AtomicBoolean(false)
    private var averageIndex = 0

    private val averageArray = DoubleArray(RED_AVG_ARRAY_SIZE)
    private var current = TYPE.GREEN
    private var beatsIndex = 0

    private val beatsArray = IntArray(BEATS_ARRAY_SIZE)
    private var beats = 0.0
    private var startTime: Long = System.currentTimeMillis()
    private var analysed_frames = 0
    private var justStarted = true

    //    RMSSD
    private var prevRPeakTime: Long = -1
    private var prevInterval: Long = -1
    private var rrIntervalsList = arrayListOf<Long>()

    //    fft
    private var imgRAvgArray = arrayListOf<Double>()
    private var imgBAvgArray = arrayListOf<Double>()

    //    o2
    private var stdB = 0.0
    private var stdR = 0.0

//    smoothening
    var svR = 0.0
    var svB = 0.0
    private var rAvgList = arrayListOf<Double>()
    private var bAvgList = arrayListOf<Double>()
    private var timeList = arrayListOf<Long>()
    companion object {
        private const val RED_AVG_ARRAY_SIZE = 4
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