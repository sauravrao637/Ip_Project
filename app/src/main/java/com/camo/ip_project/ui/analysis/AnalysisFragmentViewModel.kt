/****************************************************************************************
 * Copyright <2022> <Saurav Rao> <sauravrao637@gmail.com>                                *
 *                                                                                       *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this  *
 * software and associated documentation files (the "Software"), to deal in the Software *
 * without restriction, including without limitation the rights to use, copy, modify,    *
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to    *
 * permit persons to whom the Software is furnished to do so, subject to the following   *
 * conditions:                                                                           *
 *                                                                                       *
 * The above copyright notice and this permission notice shall be included in all copies *
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,   *
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A         *
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT    *
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF  *
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE  *
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                                         *
 *****************************************************************************************/

package com.camo.ip_project.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camo.ip_project.database.Repository
import com.camo.ip_project.database.local.model.UserHRV
import com.camo.ip_project.util.Resource
import com.camo.ip_project.util.Status
import com.camo.ip_project.util.hrv.AnalysisData
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class AnalysisFragmentViewModel @Inject constructor(
    private val repo: Repository
) : ViewModel() {
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> get() = _username

    private val _saveResponse = Channel<String>()
    val saveResponse = _saveResponse.receiveAsFlow()

    private val _saveBtnState = MutableStateFlow(false)
    val saveBtnState: StateFlow<Boolean> get() = _saveBtnState

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
            _analysedData.value = Resource.idle()
            cancelAnalysis()
        } else startAnalysis()
    }

    private fun startAnalysis() {
        _analysisState.value = true
        _analysisProgress.value = 0
        _analysedData.value = Resource.idle()
        _analysisData = AnalysisData()
        mSeries1.resetData(arrayOf())
    }

    fun updateProgress(progress: Double) {
        _analysisProgress.value = min(progress.toInt(), 100)
    }

    private fun endAnalysis() {
        _analysisProgress.value = 0
        _analysisState.value = false
    }

    private fun cancelAnalysis() {
        endAnalysis()
        reset()
    }

    private fun reset() {
        _saveBtnState.value = false
        _analysisData = null
        mSeries1.resetData(arrayOf())
    }

    fun errorInAnalysis(error: String) {
        _analysedData.value = Resource.error(errorInfo = error)
        cancelAnalysis()
    }

    private var analysisFinalDataJob: Job? = null
    fun processingComplete() {
        analysisFinalDataJob?.cancel()
        _analysedData.value = Resource.loading()
        analysisFinalDataJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                endAnalysis()
                _analysedData.value = try {
                    val data = _analysisData?.getData()
                    Timber.d("analysed")
                    if (data == null) Resource.error(errorInfo = "Something went wrong")
                    else {
                        _saveBtnState.value = true
                        Resource.success(data)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _saveBtnState.value = false
                    Resource.error(errorInfo = e.localizedMessage ?: "E")
                }
                Timber.d("ended")
            }
        }
    }

    fun signalListener(rAvg: Double, t: Long) {
        _analysisData?.addSignalData(rAvg, t)
        mSeries1.appendData(DataPoint((_analysisData?.getTime()?.size ?: 1) * 1.0, rAvg), true, 100)
    }

    fun setUsername(username: String) {
        _username.value = username
    }

    fun canSave(): Boolean {
        return (analysedData.value.status == Status.SUCCESS) && analysedData.value.data != null
    }

    fun saveData() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!canSave()) {
                _saveResponse.send("Can't save :)")
                return@launch
            }
            val data = analysedData.value.data!!
            _saveBtnState.value = false
            try {
                val id = repo.addData(
                    UserHRV(
                        userName = username.value,
                        heartRate = data.bpm,
                        sdnn = data.sd,
                        rmssd = data.rmssd,
                        nni = data.nni,
                        unixTimestamp = data.unixTimestamp
                    )
                )
                _saveResponse.send("Success id = $id")
            } catch (e: Exception) {
                e.printStackTrace()
                _saveResponse.send("Error " + (e.localizedMessage ?: "Something went wrong :)"))
            }
        }
    }
}