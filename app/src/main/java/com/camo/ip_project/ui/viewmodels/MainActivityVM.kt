package com.camo.ip_project.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camo.ip_project.database.Repository
import com.camo.ip_project.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityVM @Inject constructor(
    private val cgRepo: Repository
) : ViewModel() {
    private val _beatState = MutableStateFlow<Resource<Int>>(Resource.loading())
    val beatState: StateFlow<Resource<Any>> get() = _beatState
    private val _beat = MutableStateFlow(false)
    val beat: StateFlow<Boolean> get() = _beat
    fun setBeats(luma: Int) {
        _beatState.value = Resource.success(luma)
    }

    fun resetBeatData() {
        _beatState.value = Resource.loading()
    }

    fun beat() {
        _beat.value = true
        viewModelScope.launch {
            delay(100)
            _beat.value = false
        }
    }
}