package com.camo.ip_project.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camo.ip_project.util.Constants.MIN_RED_INTENSITY
import com.camo.ip_project.util.ImageProcessing.decodeYUV420SPtoRGBAvg
import com.camo.ip_project.util.Utility.toByteArray
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class NaiveAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        if (!processing.compareAndSet(false, true)) {
            image.close()
            return
        }
        val width = image.width
        val height = image.height
        val data = Yuv.toBuffer(image).buffer.toByteArray().clone()
        val imgAvg = decodeYUV420SPtoRGBAvg(data, width, height)[0].toInt()

        Timber.d("imgAvg: $imgAvg")

        if ((imgAvg < MIN_RED_INTENSITY) && counter>=5) {
            processing.set(false)
            image.close()
            listener(Beat.error("Bad Frames"))
            Timber.d("Bad Frame no. = $counter")
            return
        }
        counter++
        var averageArrayAvg = 0
        var averageArrayCnt = 0
        for (i in averageArray.indices) {
            if (averageArray[i] > 0) {
                averageArrayAvg += averageArray[i]
                averageArrayCnt++
            }
        }
        val rollingAverage = if (averageArrayCnt > 0) averageArrayAvg / averageArrayCnt else 0
        var newType = current
        if (imgAvg < rollingAverage) {
            newType = TYPE.RED
            if (newType != current) {
                beats++
                listener(Beat.beat())
                Timber.d("BEAT!! beats=$beats")
            }
        } else if (imgAvg > rollingAverage) {
            newType = TYPE.GREEN
        }
        if (averageIndex == averageArraySize) averageIndex = 0
        averageArray[averageIndex] = imgAvg
        averageIndex++

        // Transitioned from one state to another to the same
        if (newType != current) {
            current = newType
        }
        val endTime = System.currentTimeMillis()
        val totalTimeInSecs = (endTime - startTime) / 1000.0
        Timber.d("totalTime: $totalTimeInSecs s")
        if (totalTimeInSecs >= 10) {
            val bps = beats / totalTimeInSecs
            val bpm = (bps * 60.0).toInt()

            Timber.d("bpm:$bpm")
            if (bpm < 30 || bpm > 180) {
                startTime = System.currentTimeMillis()
                beats = 0.0
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
            val beatsAvg = if(beatsArrayCnt!=0) beatsArrayAvg / beatsArrayCnt else 0
            Timber.d("bpm:$beatsAvg")
//            if(beatsIndex <= beatsArray.size)
            listener(Beat.poorEstimationBeats(beatsAvg))
//            else listener(Beat.finalBeats(beatsAvg))
            startTime = System.currentTimeMillis()
            beats = 0.0
        }
        listener(Beat.progress((totalTimeInSecs*10).toInt()))
        processing.set(false)
        image.close()
    }

    private val processing = AtomicBoolean(false)
    private var averageIndex = 0

    private val averageArray = IntArray(averageArraySize)
    var current = TYPE.GREEN
        private set
    private var beatsIndex = 0

    private val beatsArray = IntArray(beatsArraySize)
    private var beats = 0.0
    private var startTime: Long = 0
    private var counter = 0
    companion object {
        private const val averageArraySize = 4
        private const val beatsArraySize = 3
    }
}