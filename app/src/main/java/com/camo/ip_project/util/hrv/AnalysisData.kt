package com.camo.ip_project.util.hrv

import com.camo.ip_project.util.Utility
import com.github.psambit9791.jdsp.filter.Butterworth
import com.github.psambit9791.jdsp.misc.UtilMethods
import com.github.psambit9791.jdsp.signal.Smooth
import com.github.psambit9791.jdsp.signal.peaks.FindPeak
import com.github.psambit9791.jdsp.splines.CubicSpline
import timber.log.Timber

class AnalysisData {
    private val signal = arrayListOf<Double>()
    private val t = arrayListOf<Double>()
    private var analysedData: AnalysedData? =null
    private var analysed = false
    private fun analyse() {
        analysedData = analyseDataForHrvDefault(signal,t)
        analysed = true
    }

    fun getData(): AnalysedData? {
        if(analysed) return analysedData
        analyse()
        return analysedData
    }

    fun addSignalData(yValue: Double, xValue: Long){
        signal.add(yValue)
        t.add(xValue*1.0)
    }

    fun getSignal(): java.util.ArrayList<Double> {
        return signal
    }

    fun getTime(): java.util.ArrayList<Double> {
        return t
    }

    companion object {
        private fun analyseDataForHrvDefault(signal: ArrayList<Double>, t: ArrayList<Double>): AnalysedData {
            val startTime = t[0]
            val endTime = t[t.size - 1]
            val totalDuration = (endTime - startTime) / 1000.0
            Timber.d("totalDuration: $totalDuration")

            if (totalDuration < 60) {
                throw SampleVerySmallException()
            }
//            Filter the data with 4th order Butterworth Bandpass Filter (lower cutoff freq = 0.667Hz and higher cutoff freq = 3.5Hz)
            val sf = signal.size / totalDuration
            val filter = Butterworth(signal.toDoubleArray(), sf)
            val filteredSignal = filter.bandPassFilter(4, 0.667, 3.5)

//            Smoothen the signal with triangular mode (window size = 11)
            val mode = "triangular"
            val wSize = 11
            val s1 = Smooth(filteredSignal, wSize, mode)
            val smoothenedSignal = s1.smoothSignal().toMutableList()

//            trim timestamps to size of smoothened signal
            val tConcatenated = t.toMutableList()
            while (tConcatenated.size > smoothenedSignal.size) {
                tConcatenated.removeLastOrNull()
            }
//            Remove first 20s signal considering it as unreliable
            while (tConcatenated[0] - startTime < 20000.0) {
                tConcatenated.removeFirstOrNull()
                smoothenedSignal.removeFirstOrNull()
            }
            val newStartTime = tConcatenated[0]
            val newEndTime = tConcatenated[tConcatenated.size - 1]

//            resample with cubic spline at 200Hz
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

//            find peaks
            val fp = FindPeak(resampledSignal)
            val output = fp.detectPeaks()
            val peaks = output.peaks

//            calculating intervals (interval of 1 unit corresponds to 5ms when resampled at 200Hz)
            val intervals = arrayListOf<Double>()
            for (i in 1 until peaks.size) {
                intervals.add(1.0 * (peaks[i] - peaks[i - 1]))
            }
            val vr = Utility.getVarianceDouble(intervals)
            val tPerFrame = newTotalTimeMs / resampledTimeStamps.size
            val rmssd = vr * tPerFrame

            val bpm = (60 * peaks.size / (newTotalTimeMs / 1000)).toInt()
            Timber.d("ts $newStartTime te $newEndTime t $tPerFrame vr: $vr bpm: $bpm, rmssd $rmssd")
            return AnalysedData(rmssd.toInt(), bpm)
        }
        class SampleVerySmallException : Exception("Sample size less than 60s")
        data class AnalysedData(val rmssd: Int, val bpm: Int)
    }
}