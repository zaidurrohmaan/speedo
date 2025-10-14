package com.zaidu.speedo

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// State untuk UI
data class SpeedoUiState(
    val speed: Float = 0f, // Kecepatan dalam m/s
    val distance: Float = 0f, // Jarak dalam meter
    val elapsedTime: Long = 0, // Waktu dalam detik
    val isRunning: Boolean = false,
    val hasStarted: Boolean = false,
    val hasStopped: Boolean = false
)

class SpeedoViewModel(application: Application) : AndroidViewModel(application) {

    private val locationService = LocationService(application)
    private var locationJob: Job? = null
    private var timerJob: Job? = null
    private var lastLocation: Location? = null

    private val _uiState = MutableStateFlow(SpeedoUiState())
    val uiState: StateFlow<SpeedoUiState> = _uiState.asStateFlow()

    fun startTracking() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true, hasStarted = true, distance = 0f, elapsedTime = 0) }
        startTimer()
        startLocationUpdates()
    }

    fun resumeTracking() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true) }
        startTimer()
        startLocationUpdates()
    }

    fun pauseTracking() {
        if (!_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = false) }
        stopJobs()
    }

    fun stopTracking() {
        _uiState.update { it.copy(isRunning = false, hasStarted = false, hasStopped = true) }
        lastLocation = null
        stopJobs()
    }

    fun resetTracking() {
        _uiState.value = SpeedoUiState()
    }

    private fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = locationService.locationUpdates
            .catch { e ->
                // Handle error, e.g., permissions denied
                e.printStackTrace()
            }
            .onEach { newLocation ->
                val currentSpeed = newLocation.speed
                val currentDistance = _uiState.value.distance + (lastLocation?.distanceTo(newLocation) ?: 0f)

                _uiState.update {
                    it.copy(speed = currentSpeed, distance = currentDistance)
                }
                lastLocation = newLocation
            }
            .launchIn(viewModelScope)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(1000)
                _uiState.update { it.copy(elapsedTime = it.elapsedTime + 1) }
            }
        }
    }

    private fun stopJobs() {
        timerJob?.cancel()
        locationJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopJobs()
    }
}
