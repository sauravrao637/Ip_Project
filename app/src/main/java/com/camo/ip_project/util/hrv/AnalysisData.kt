/*****************************************************************************************
 * Copyright <2022> <Saurav Rao> <sauravrao637@gmail.com>                                *
 * Copyright <2022> <Piyush Sharma> <piyushlp@gmail.com>                                 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this  *
 * software and associated documentation files (the "Software"), to deal in the Software *
 * without restriction, including without limitation the rights to use, copy, modify,    *
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to    *
 * permit persons to whom the Software is furnished to do so, subject to the following   *
 * conditions:                                                                           *
 *                                                                                       *
 * The above copyright notice and this permission notice shall be included in all copies *
 * or substantial portions of the Software.                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,   *
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A         *
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT    *
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF  *
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE  *
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                                         *
 *****************************************************************************************/

package com.camo.ip_project.util.hrv

import com.camo.ip_project.util.Extras
import com.camo.ip_project.util.hrv.AnalysisData.Companion.AnalysedData
import com.camo.ip_project.util.hrv.AnalysisUtility.HIGH_CO
import com.camo.ip_project.util.hrv.AnalysisUtility.LOW_CO
import com.camo.ip_project.util.hrv.AnalysisUtility.SAMPLE_SIZE
import com.camo.ip_project.util.hrv.AnalysisUtility.UNRELIABLE_SIGNAL_DURATION
import com.github.psambit9791.jdsp.filter.Butterworth
import com.github.psambit9791.jdsp.misc.UtilMethods
import com.github.psambit9791.jdsp.signal.Smooth
import com.github.psambit9791.jdsp.signal.peaks.FindPeak
import com.github.psambit9791.jdsp.splines.CubicSpline
import com.jjoe64.graphview.series.DataPoint
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * This class is responsible for analysing various heart rate variability factors using the red
 * intensities and corresponding timestamps
 *
 * @see AnalysedData
 */
class AnalysisData {
    private val signal = arrayListOf<Double>()
    private val t = arrayListOf<Double>()
    private lateinit var analysedData: AnalysedData

    private fun analyse() {
        analysedData = analyseDataForHrvDefault(signal, t)
    }

    fun getData(): AnalysedData {
        if (this::analysedData.isInitialized) return analysedData
        analyse()
        return analysedData
    }

    fun addSignalData(yValue: Double, xValue: Long) {
        signal.add(yValue)
        t.add(xValue * 1.0)
    }

    fun getSignal(): java.util.ArrayList<Double> {
        return signal
    }

    fun getTime(): java.util.ArrayList<Double> {
        return t
    }

    companion object {
        /** This takes signal(red intensity array) and corresponding timestamp and analyze it for
         * different hrv data
         *
         * Methodology :-
         *
         * 1-> Filter the data with 4th order Butterworth Bandpass Filter with low and high cutoff
         * frequencies
         *
         * 2-> Smoothen the signal with triangular mode (window size = 11)
         *
         * 3-> Remove first few seconds of signal considering it as unreliable
         *
         * 4-> Resample with cubic spline at 200Hz
         *
         * 5-> Find peaks and calculate intervals and get different data accordingly
         *
         * @param signal: red intensity of frames
         * @param timestamps: timestamps of corresponding frames
         *
         * @throws SampleVerySmallException: when sample size is less than the required size
         *
         * @return AnalysedData
         */
        fun analyseDataForHrvDefault(
            signal: ArrayList<Double>,
            timestamps: ArrayList<Double>
        ): AnalysedData {
            val timestamp = System.currentTimeMillis()
            var toWrite = "\n\n %${timestamp}"
            var startTime = timestamps[0]
            val t = mutableListOf<Double>()
            for (i in timestamps) {
                t.add(i - startTime)
            }
            startTime = 0.0
            val endTime = t[t.size - 1]
            val totalDuration = (endTime - startTime) / 1000.0
            Timber.d("totalDuration: $totalDuration")
            toWrite += "\nsignal= $signal;\ntime= $t;"
            toWrite += "\ntotalDuration= $totalDuration;"

            if (totalDuration < SAMPLE_SIZE) {
                throw SampleVerySmallException()
            }
//            Filter the data with 4th order Butterworth Bandpass Filter
//            (lower cutoff freq = LOW_CO and higher cutoff freq = HIGH_CO)
            val sf = signal.size / totalDuration
            val filter = Butterworth(signal.toDoubleArray(), sf)
            val filteredSignal = filter.bandPassFilter(4, LOW_CO, HIGH_CO)
            toWrite += "\nfiltered= ${filteredSignal.toList()};"

//            Smoothen the signal with triangular mode (window size = 11)
            val mode = "triangular"
            val wSize = 11
            val s1 = Smooth(filteredSignal, wSize, mode)
            val smoothenedSignal = s1.smoothSignal().toMutableList()
            toWrite += "\nsmoothenedSignal= $smoothenedSignal;"

//            trim timestamps to size of smoothened signal
            val tConcatenated = t.toMutableList()
            while (tConcatenated.size > smoothenedSignal.size) {
                tConcatenated.removeFirst()
            }
            toWrite += "\nsmoothenedSignalTime = $tConcatenated;"

//            Remove first few seconds of signal considering it as unreliable
            while (tConcatenated[0] - startTime < UNRELIABLE_SIGNAL_DURATION * 1000) {
                tConcatenated.removeFirst()
                smoothenedSignal.removeFirst()
            }
            val newStartTime = tConcatenated[0]
            val newEndTime = tConcatenated[tConcatenated.size - 1]

//            Resample with cubic spline at 200Hz
            val cbs = CubicSpline()
            cbs.computeFunction(tConcatenated.toDoubleArray(), smoothenedSignal.toDoubleArray())
            val newTotalTimeMs = newEndTime - newStartTime
            val resampledTimeStamps = UtilMethods.linspace(
                newStartTime * 1.0,
                newEndTime * 1.0,
                (newTotalTimeMs / 5).toInt(),
                true
            )
            val resampledSignal = cbs.getValue(resampledTimeStamps)
            toWrite += "\nresampledSignal= ${resampledSignal.toList()};"
            toWrite += "\nresampledTime= ${resampledTimeStamps.toList()};"

//            find peaks
            val fp = FindPeak(resampledSignal)
            val output = fp.detectPeaks()
            val peaks = output.peaks
            toWrite += "\npeaks= ${peaks.toList()};"

//            calculating intervals (interval of 1 unit corresponds to 5ms when resampled at 200Hz)
            val intervals = arrayListOf<Double>()
            for (i in 1 until peaks.size) {
                intervals.add(5.0 * (peaks[i] - peaks[i - 1]))
            }
            toWrite += "\nintervals= $intervals;"

            val tPerFrame = newTotalTimeMs / resampledTimeStamps.size
            var meanNNI = 0.0
            for (i in intervals) {
                meanNNI += (i / intervals.size)
            }

            val vr = Extras.getVarianceDouble(intervals)
            val sd = sqrt(vr)
            val rmssd = getRmssd(intervals)

            val bpm = (60 * peaks.size / (newTotalTimeMs / 1000)).toInt()

            Timber.d(
                "ts $newStartTime te $newEndTime t $tPerFrame vr: $vr bpm: $bpm %s",
                "rmssd $rmssd, meanNNI: $meanNNI, sd: $sd"
            )

            toWrite += "\n"
            toWrite += "rmssd = $rmssd;\nbpm = $bpm;\nNNI = $meanNNI;\nsd = $sd;"

            val resampledData = Array(resampledSignal.size) {
                DataPoint(resampledTimeStamps[it] / 1000, resampledSignal[it])
            }

            return AnalysedData(
                rmssd.toInt(),
                bpm,
                meanNNI.toInt(),
                sd.toInt(),
                toWrite,
                resampledData,
                timestamp
            )
        }

        /**
         * Calculates RMSSD for RR intervals
         * RMSSD is  root mean square of successive differences between normal heartbeats
         * @param intervals: RR intervals
         * @return RMSSD
         */
        fun getRmssd(intervals: ArrayList<Double>): Double {
            var ans = 0.0
            for (i in 1 until intervals.size) {
                ans += ((intervals[i] - intervals[i - 1]).pow(2))
            }
            ans /= (intervals.size - 1)
            ans = ans.pow(0.5)
            return ans
        }

        class SampleVerySmallException : Exception("Sample size less than $SAMPLE_SIZE s")

        /**
         * Responsible for storing in it the analysed data after signal processing is done
         * @param rmssd: root mean square of successive differences between normal heartbeats
         * @param bpm: beats per minute
         * @param sd: standard deviation of NN intervals (SDNN)
         * @param nni: NN index (average NN interval)
         * @param unixTimestamp: timestamp of analysis
         * @param resampledData: signal after processing is done
         * @param outText: text that can be useful for matlab script debugging and also include
         * results for the analysis
         */
        data class AnalysedData(
            val rmssd: Int,
            val bpm: Int,
            val nni: Int,
            val sd: Int,
            val outText: String,
            val resampledData: Array<DataPoint>,
            val unixTimestamp: Long
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as AnalysedData

                if (rmssd != other.rmssd) return false
                if (bpm != other.bpm) return false
                if (nni != other.nni) return false
                if (sd != other.sd) return false
                if (outText != other.outText) return false
                if (!resampledData.contentEquals(other.resampledData)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = rmssd
                result = 31 * result + bpm
                result = 31 * result + nni
                result = 31 * result + sd
                result = 31 * result + outText.hashCode()
                result = 31 * result + resampledData.contentHashCode()
                return result
            }
        }
    }
}