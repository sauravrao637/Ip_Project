package com.camo.ip_project.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camo.ip_project.database.Repository
import com.camo.ip_project.util.Beat
import com.camo.ip_project.util.BeatDataType
import com.camo.ip_project.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class MainActivityVM @Inject constructor(
    private val cgRepo: Repository
) : ViewModel() {
    private val _beatState = MutableStateFlow<Resource<Beat.BeatData>>(Resource.idle())
    val beatState: StateFlow<Resource<Beat.BeatData>> get() = _beatState

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> get() = _progress

    private val _beat = MutableStateFlow(false)
    val beat: StateFlow<Boolean> get() = _beat


    fun resetBeatData() {
        _beatState.value = Resource.idle()
        _progress.value = 0
    }
    fun beatState(beat: Beat){
        when(beat.beatDataType){
            BeatDataType.Beat->{
                beat()
            }
            BeatDataType.Error->{
                _beatState.value = Resource.error(null,beat.error?:"E")
                _progress.value = 0
            }
            BeatDataType.Estimated->{
                _beatState.value = Resource.loading(beat.data)
            }
            BeatDataType.Progress->{
                _progress.value = min(beat.progress?:0,100)
            }
            BeatDataType.FINAL ->{
                _beatState.value = Resource.success(beat.data!!)
                _progress.value = 0
            }
            BeatDataType.ANALYSIS -> {
                Timber.d("imgAvg: ${beat.imgAvg}, time: ${beat.time}")
            }
        }
    }

//    run beating animation
    private fun beat() {
        _beat.value = true
        viewModelScope.launch {
            delay(100)
            _beat.value = false
        }
    }
}

//class BeatData {
//    private var bpm = 0
//    private var rmssd = 0.0
//    fun setRmssd(rmssd: Double) {
//        this.rmssd = rmssd
//    }
//    fun setBpm(bpm: Int){
//        this.bpm = bpm
//    }
//
//    fun getRmssd(): Double = rmssd
//    fun getBpm(): Int = bpm
//}
