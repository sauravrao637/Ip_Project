package com.camo.ip_project.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camo.ip_project.database.Repository
import com.camo.ip_project.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainActivityVM @Inject constructor(
    private val cgRepo: Repository
) : ViewModel() {
    private val _beatState = MutableStateFlow<Resource<Double>>(Resource.loading())
    val beatState: StateFlow<Resource<Any>> get() = _beatState

    fun setBeats(luma: Double) {
        _beatState.value = Resource.success(luma)
    }
}