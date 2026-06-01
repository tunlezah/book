package com.dogear.reader.feature.upload.server

import com.dogear.reader.core.ingest.ImportResult
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.newFixedLengthResponse
import java.io.File
import java.security.MessageDigest

/**
 * Minimal local-network upload server (NanoHTTPD). Security controls (Security Review §E):
 * binds to the LAN address only; optional password checked with a constant-time comparison;
 * client-supplied paths are ignored (the importer generates safe names); request size is capped
 * before the body is read; uploads are validated by content inside the importer. Serves a single
 * no-cookie form and one upload endpoint — nothing else.
 */
internal class UploadServer(
    hostname: String?,
    port: Int,
    private val password: String?,
    private val onUploaded: (name: String, file: File) -> ImportResult,
) : NanoHTTPD(hostname, port) {

    override fun serve(session: IHTTPSession): Response = when {
        session.method == Method.GET && session.uri == "/" -> formResponse()
        session.method == Method.POST && session.uri == "/upload" -> handleUpload(session)
        else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
    }

    private fun handleUpload(session: IHTTPSession): Response {
        val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0
        if (contentLength > MAX_UPLOAD_BYTES) {
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "File too large",
            )
        }

        val files = HashMap<String, String>()
        runCatching { session.parseBody(files) }.onFailure {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Bad request")
        }
        val params = session.parameters

        if (!password.isNullOrEmpty()) {
            val provided = params["password"]?.firstOrNull().orEmpty()
            if (!constantTimeEquals(provided, password)) {
                return newFixedLengthResponse(
                    Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Incorrect password",
                )
            }
        }

        if (files.isEmpty()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No file")
        }

        var imported = 0
        var skipped = 0
        files.forEach { (field, tempPath) ->
            // The original filename arrives as the parameter value; the importer ignores any path.
            val rawName = params[field]?.firstOrNull() ?: "upload.bin"
            val safeName = rawName.substringAfterLast('/').substringAfterLast('\\')
            when (onUploaded(safeName, File(tempPath))) {
                is ImportResult.Imported -> imported++
                is ImportResult.Archive -> imported++
                else -> skipped++
            }
        }
        val message = "Imported $imported file(s)" + if (skipped > 0) ", skipped $skipped" else ""
        return newFixedLengthResponse(Response.Status.OK, "text/html", resultPage(message))
    }

    private fun formResponse(): Response =
        newFixedLengthResponse(Response.Status.OK, "text/html", FORM_HTML)

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

    private fun resultPage(message: String): String = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Dogear</title></head><body style="font-family:sans-serif;max-width:520px;margin:40px auto;padding:0 16px">
        <h2>$message</h2><p><a href="/">Upload more</a></p></body></html>
    """.trimIndent()

    private companion object {
        const val MAX_UPLOAD_BYTES = 512L * 1024 * 1024

        val FORM_HTML = """
            <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Dogear — Upload</title></head>
            <body style="font-family:sans-serif;max-width:520px;margin:40px auto;padding:0 16px">
              <h2>Send books to Dogear</h2>
              <form action="/upload" method="post" enctype="multipart/form-data">
                <p><input type="file" name="book" multiple></p>
                <p>Password (if set): <input type="password" name="password"></p>
                <p><button type="submit">Upload</button></p>
              </form>
              <p style="color:#666">EPUB, PDF, TXT and CBZ are supported.</p>
            </body></html>
        """.trimIndent()
    }
}
