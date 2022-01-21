package com.camo.ip_project.util

import android.provider.Settings
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camo.ip_project.util.Constants.MAX_HR_RANGE
import com.camo.ip_project.util.Constants.MIN_HR_RANGE
import com.camo.ip_project.util.Constants.MIN_RED_INTENSITY
import com.camo.ip_project.util.Constants.SAMPLING_PERIOD
import com.camo.ip_project.util.ImageProcessing.fft
import com.camo.ip_project.util.Utility.toByteArray
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil


class HRAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
    // Variables Initialization
    private val TAG = "HeartRateMonitor"
    private val processing: AtomicBoolean = AtomicBoolean(false)

    //Beats variable
    private var beatsPm = 0
    private var bufferAvgB = 0.0

    //Freq + timer variable
    private var startTime: Long = System.currentTimeMillis()
    private var samplingFreq = 0.0

    //Arraylist
    private var greenAvgList = ArrayList<Double>()
    private var redAvgList = ArrayList<Double>()
    private var counter = 0

    var ProgP = 0
    var inc = 0

    init {
        Timber.d("HRAnalyzer")
    }

    override fun analyze(image: ImageProxy) {
//Atomically sets the value to the given updated value if the current value == the expected value.
        if (!processing.compareAndSet(false, true)) {
            image.close()
            return
        }

        val width: Int = image.width
        val height: Int = image.height
        val data = Yuv.toBuffer(image).buffer.toByteArray().clone()
        val avgArray = ImageProcessing.decodeYUV420SPtoRGBAvg(data, height, width)
        val greenAvg = avgArray[1]
        val redAvg = avgArray[0]

        greenAvgList.add(greenAvg);
        redAvgList.add(redAvg)
        ++counter

        if ((redAvg < MIN_RED_INTENSITY) && counter>16) {
            counter = 0
            inc = 0
            ProgP = inc
            listener(Beat.progress(ProgP))
//            listener(Beat.error("bad frame"))
            processing.set(false)
//            image.close()
//            return
        }
        val endTime = System.currentTimeMillis()
        val totalTimeInSecs = (endTime - startTime) / 1000.0

        if (totalTimeInSecs >= SAMPLING_PERIOD) {
            val green: Array<Double> = greenAvgList.toArray(arrayOfNulls<Double>(greenAvgList.size))
            val red: Array<Double> = redAvgList.toArray(arrayOfNulls<Double>(redAvgList.size))

            samplingFreq = (counter / totalTimeInSecs)

            val hRFreqG: Double = fft(green, counter, samplingFreq)
            val bpmG: Double = ceil(hRFreqG * 60)

            val hRFreqR: Double = fft(red, counter, samplingFreq)
            val bpmR: Double = ceil(hRFreqR * 60)

//             The following code is to make sure that if the heartrate from red and green intensities are reasonable
//             take the average between them, otherwise take the green or red if one of them is good
            if ((bpmG > MIN_HR_RANGE || bpmG < MAX_HR_RANGE)) {
                bufferAvgB = if ((bpmR > MIN_HR_RANGE || bpmR < MAX_HR_RANGE)) {
                    (bpmG + bpmR) / 2
                } else {
                    bpmG
                }
            } else if ((bpmR > MIN_HR_RANGE || bpmR < MAX_HR_RANGE)) {
                bufferAvgB = bpmR
            }

            if (bufferAvgB < MIN_HR_RANGE || bufferAvgB > MAX_HR_RANGE) {
                Timber.d("Measurement Failed, bufferAvgB = $bufferAvgB")
                counter = 0
                inc = 0;
                ProgP = inc;
                listener(Beat.progress(ProgP))
                startTime = System.currentTimeMillis()
                listener(Beat.error(null))
                processing.set(false)
                image.close()
                return
            }
            beatsPm = bufferAvgB.toInt()
        }

        if (beatsPm != 0) {
//            if beasts were reasonable stop the loop and send HR
//            Beats calculated
//            listener(if(totalTimeInSecs>=30) Beat.finalBeats(beatsPm) else Beat.poorEstimationBeats(beatsPm))
            listener(Beat.finalBeats(beatsPm))
        }
        if(redAvg !=0.0){
            ProgP = inc++/34
            listener(Beat.progress(ProgP))
        }
        processing.set(false)
        image.close()
    }
}
/*
package com.camo.ip_project.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.camo.ip_project.util.Utility.FFT
import com.camo.ip_project.util.Utility.toByteArray
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil


class HRAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
    // Variables Initialization
    private val TAG = "HeartRateMonitor"
    private val processing: AtomicBoolean = AtomicBoolean(false)

    //Beats variable
    var Beats = 0
    var bufferAvgB = 0.0

    //ProgressBar
    private val progress: Int = 0
    var ProgP = 0
    var inc = 0

    //Freq + timer variable
    private var startTime: Long = System.currentTimeMillis()
    private var SamplingFreq = 0.0

    //Arraylist
    var greenAvgList = ArrayList<Double>()
    var redAvgList = ArrayList<Double>()
    var counter = 0

    override fun analyze(image: ImageProxy) {
//Atomically sets the value to the given updated value if the current value == the expected value.
        if (!processing.compareAndSet(false, true)) return;
        val greenAvg: Double
        val redAvg: Double

        val width: Int = image.width
        val height: Int = image.height
        val data = Yuv.toBuffer(image).buffer.toByteArray()
        val avgArray = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(
            data.clone(),
            height,
            width)
        greenAvg = avgArray[1]

        redAvg = avgArray[0]

        greenAvgList.add(greenAvg);
        redAvgList.add(redAvg);

        ++counter;//1 stands for red intensity, 2 for blue, 3 for green

        //To check if we got a good red intensity to process if not return to the condition and set it again until we get a good red intensity
        if (redAvg < 200) {
            inc = 0;
            ProgP = inc;
            counter = 0;
//            update progress
            listener(Beat.progress(ProgP))
            processing.set(false);
            image.close()
        }

        val endTime = System.currentTimeMillis()
        val totalTimeInSecs = (endTime - startTime) / 1000.0 //to convert time to seconds

        if (totalTimeInSecs >= 30) {
//            when 30 seconds of measuring passes do the following " we chose 30 seconds to take half sample since 60 seconds is normally a full sample of the heart beat
            val green: Array<Double> =
                greenAvgList.toArray(arrayOfNulls<Double>(greenAvgList.size))
            val red: Array<Double> = redAvgList.toArray(arrayOfNulls<Double>(redAvgList.size))
            SamplingFreq = counter / totalTimeInSecs //calculating the sampling frequency
            val hRFreq: Double = FFT(
                green,
                counter,
                SamplingFreq
            )
//             send the green array and get its fft then return the amount of heartrate per second
            val bpm: Double = ceil(hRFreq * 60)
            val hR1Freq: Double = FFT(
                red,
                counter,
                SamplingFreq
            ) // send the red array and get its fft then return the amount of heartrate per second
            val bpm1: Double = ceil(hR1Freq * 60)

            // The following code is to make sure that if the heartrate from red and green intensities are reasonable
            // take the average between them, otherwise take the green or red if one of them is good
            if (bpm > 45 || bpm < 200) {
                bufferAvgB = if (bpm1 > 45 || bpm1 < 200) {
                    (bpm + bpm1) / 2
                } else {
                    bpm
                }
            } else if (bpm1 > 45 || bpm1 < 200) {
                bufferAvgB = bpm1
            }

            if (bufferAvgB < 45 || bufferAvgB > 200) {
//                if the heart beat wasn't reasonable after all reset the progresspag and restart measuring
                inc = 0
                ProgP = inc
//                update progress
                listener(Beat.progress(ProgP))
                Timber.d("Measurement Failed")
                startTime = System.currentTimeMillis()
                counter = 0
                processing.set(false)
                image.close()
                return
            }
            Beats = bufferAvgB.toInt()
        }

        if (Beats != 0) {
//            if beasts were reasonable stop the loop and send HR with the username to results activity and finish this activity
//            Beats calculated
            listener(Beat.finalBeats(Beats))
        }


        if (redAvg != 0.0) {
//            increment the progresspar
            ProgP = inc++ / 34
//            update progress
            listener(Beat.progress(ProgP))
        }

//        keeps taking frames till 30 seconds
        processing.set(false)
        image.close()
    }
}
 */