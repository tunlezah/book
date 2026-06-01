package com.dogear.reader.feature.upload.server

import com.dogear.reader.core.ingest.BookImporter
import com.dogear.reader.core.ingest.ImportResult
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Running state of the upload server, observed by the UI. */
data class ServerStatus(
    val running: Boolean = false,
    val url: String? = null,
    val uploadedCount: Int = 0,
    val error: String? = null,
)

/**
 * Owns the upload server lifecycle (singleton, so it survives screen recomposition). Binds to the
 * LAN address only; each uploaded file is run through the shared [BookImporter] (validation,
 * dedupe, cover) and the shelf refreshes automatically via its Room Flow.
 */
@Singleton
class UploadServerController @Inject constructor(
    private val importer: BookImporter,
) {
    private val _status = MutableStateFlow(ServerStatus())
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    private var server: UploadServer? = null

    fun start(port: Int, password: String?) {
        if (_status.value.running) return
        val host = Network.localIpAddress()
        if (host == null) {
            _status.update { it.copy(error = "Connect to Wi‑Fi to start the server") }
            return
        }
        try {
            val srv = UploadServer(host, port, password?.ifBlank { null }, ::onUploaded)
            srv.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            server = srv
            _status.value = ServerStatus(running = true, url = "http://$host:$port", error = null)
        } catch (t: Throwable) {
            _status.update { it.copy(running = false, error = t.message ?: "Could not start server") }
        }
    }

    fun stop() {
        runCatching { server?.stop() }
        server = null
        _status.update { it.copy(running = false, url = null) }
    }

    private fun onUploaded(name: String, file: File): ImportResult {
        val result = runBlocking { importer.importFromStream(file.inputStream(), name) }
        if (result is ImportResult.Imported || result is ImportResult.Archive) {
            _status.update { it.copy(uploadedCount = it.uploadedCount + 1) }
        }
        return result
    }
}
