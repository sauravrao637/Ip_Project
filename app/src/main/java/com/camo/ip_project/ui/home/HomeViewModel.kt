package com.camo.ip_project.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camo.ip_project.database.Repository
import com.camo.ip_project.ui.Utility
import com.camo.ip_project.util.Resource
import com.camo.ip_project.util.hrv.AnalysisData
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cgRepo: Repository,
    private val context: Application
) : ViewModel() {
    private var lock = false
    private val _analysisState = MutableStateFlow(false)
    val analysisState: StateFlow<Boolean> get() = _analysisState

    private val _analysedData =
        MutableStateFlow<Resource<AnalysisData.Companion.AnalysedData>>(Resource.idle())

    val analysedData: StateFlow<Resource<AnalysisData.Companion.AnalysedData>> get() = _analysedData

    private val _analysisProgress = MutableStateFlow(0)
    val analysisProgress: StateFlow<Int> get() = _analysisProgress

    private var _analysisData: AnalysisData? = null

    var mSeries1 = LineGraphSeries<DataPoint>()

    fun toggleAnalysis() {
        if (_analysisState.value) {
            cancelAnalysis()
        } else startAnalysis()
    }

    private fun startAnalysis() {
        if (lock) return
        _analysisState.value = true
        _analysisProgress.value = 0
        _analysedData.value = Resource.idle()
        _analysisData = AnalysisData()
    }

    fun updateProgress(progress: Double) {
        _analysisProgress.value = min(progress.toInt(), 100)
    }

    private fun endAnalysis() {
        _analysisProgress.value = 0
        _analysisState.value = false
    }

    private fun cancelAnalysis() {
        if(lock) return
        endAnalysis()
        reset()
    }

    private fun reset() {
        _analysisData = null
        mSeries1.resetData(arrayOf())
    }

    fun errorInAnalysis(error: String) {
        _analysedData.value = Resource.error(errorInfo = error)
        cancelAnalysis()
    }

    private var analysisFinalDataJob: Job? = null
    @OptIn(DelicateCoroutinesApi::class)
    fun processingComplete() {
        analysisFinalDataJob?.cancel()
        _analysedData.value = Resource.loading()
        GlobalScope.launch {
            lock = true
            Utility.saveHrvData(
                context,
                _analysisData!!.getSignal(),
                _analysisData!!.getTime()
            )
            lock = false
        }
        analysisFinalDataJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                endAnalysis()
                lock = true
                _analysedData.value = try {
                    val data = _analysisData?.getData()
                    Timber.d("analysed")
                    if (data == null) Resource.error(errorInfo = "Something went wrong")
                    else Resource.success(data)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Resource.error(errorInfo = e.localizedMessage ?: "E")
                }
                lock = false
                Timber.d("ended")
            }
        }
    }

    fun signalListener(rAvg: Double, t: Long) {
        _analysisData?.addSignalData(rAvg, t)
        mSeries1.appendData(DataPoint((_analysisData?.getTime()?.size?:1)*1.0,rAvg),true,100)
    }

}