package com.dogear.reader.feature.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dogear.reader.feature.upload.server.ServerStatus
import com.dogear.reader.feature.upload.server.UploadServerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val status: ServerStatus = ServerStatus(),
    val port: String = "8080",
    val password: String = "",
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val controller: UploadServerController,
) : ViewModel() {

    private val port = MutableStateFlow("8080")
    private val password = MutableStateFlow("")

    val uiState = combine(controller.status, port, password) { status, p, pw ->
        UploadUiState(status, p, pw)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UploadUiState())

    fun setPort(value: String) {
        port.value = value.filter { it.isDigit() }.take(5)
    }

    fun setPassword(value: String) {
        password.value = value
    }

    fun start() {
        val portNumber = port.value.toIntOrNull()?.takeIf { it in 1024..65535 } ?: 8080
        viewModelScope.launch(Dispatchers.IO) { controller.start(portNumber, password.value) }
    }

    fun stop() {
        viewModelScope.launch(Dispatchers.IO) { controller.stop() }
    }
}
