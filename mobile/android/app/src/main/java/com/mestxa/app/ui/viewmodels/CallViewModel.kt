package com.mestxa.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mestxa.app.webrtc.WebRtcCallManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val callManager = WebRtcCallManager.getInstance(application.applicationContext)

    private val _callSeconds = MutableStateFlow(0)
    val callSeconds: StateFlow<Int> = _callSeconds.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    // 5 real-time bar amplitudes driven directly by the 48 kHz WebRTC acoustic stream
    private val _waveformBars = MutableStateFlow(listOf(16f, 36f, 22f, 42f, 28f))
    val waveformBars: StateFlow<List<Float>> = _waveformBars.asStateFlow()

    init {
        // Start call timer
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _callSeconds.value += 1
            }
        }

        // Connect acoustic energy flow to dynamic waveform bars
        viewModelScope.launch {
            callManager.acousticEnergy.collect { energy ->
                val base = 10f
                _waveformBars.value = listOf(
                    base + energy * 20f,
                    base + energy * 38f,
                    base + energy * 26f,
                    base + energy * 44f,
                    base + energy * 30f
                )
            }
        }
    }

    fun startCall(calleeIdentityHex: String) {
        callManager.startCall(calleeIdentityHex)
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun endCall(onEnded: () -> Unit) {
        callManager.hangup()
        onEnded()
    }
}
