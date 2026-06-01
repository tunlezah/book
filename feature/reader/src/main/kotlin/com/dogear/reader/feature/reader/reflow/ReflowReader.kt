package com.dogear.reader.feature.reader.reflow

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.dogear.reader.core.model.Locator
import com.dogear.reader.feature.reader.ReaderCommand
import com.dogear.reader.format.api.content.BookContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume

/**
 * EPUB/HTML/TXT reflow renderer. A single locked-down WebView paginates the current spine
 * document with CSS columns (see [ReaderAssets]); the host drives page turns and crosses spine
 * boundaries. Security: JS is on only for our injected pager, publisher scripts are already
 * stripped, file access is disabled, and **all** non-local requests are denied — resources are
 * served solely from inside the book via the asset interceptor.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun ReflowReader(
    content: BookContent.Reflowable,
    initialLocator: Locator?,
    style: ReflowStyle,
    commands: SharedFlow<ReaderCommand>,
    onProgress: (Locator) -> Unit,
) {
    val context = LocalContext.current
    var spineCount by remember { mutableIntStateOf(1) }
    var currentSpine by remember { mutableIntStateOf(initialLocator?.spineIndex ?: 0) }
    val pendingFraction = remember { mutableFloatStateOf(0f) }
    val bridge = remember { PaginationBridge() }

    val webView = remember {
        WebView(context).apply {
            configureForReading()
            addJavascriptInterface(bridge, "DogearBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = interceptResource(content, request)

                override fun onPageFinished(view: WebView, url: String?) {
                    // Restore the saved position once the document is laid out.
                    view.evaluateJavascript(
                        "window.DogearPager && DogearPager.goToFraction(${pendingFraction.floatValue});",
                        null,
                    )
                }
            }
        }
    }

    // Bridge reports page counts after each layout/move → persist a DOM-stable locator.
    LaunchedEffect(bridge) {
        bridge.events.collect { (count, current) ->
            val docFraction = if (count <= 1) 0f else current.toFloat() / (count - 1)
            val overall = if (spineCount <= 0) 0f else (currentSpine + docFraction) / spineCount
            onProgress(Locator(spineIndex = currentSpine, progression = overall.coerceIn(0f, 1f)))
        }
    }

    LaunchedEffect(Unit) {
        spineCount = content.spine().size.coerceAtLeast(1)
        val initFraction = initialLocator
            ?.let { (it.progression * spineCount - currentSpine).coerceIn(0f, 1f) }
            ?: 0f
        loadSpine(webView, content, currentSpine, initFraction, style, pendingFraction)
    }

    LaunchedEffect(commands) {
        commands.collect { command ->
            when (command) {
                ReaderCommand.Next -> {
                    val moved = webView.evalBoolean("window.DogearPager ? DogearPager.next() : false")
                    if (!moved && currentSpine < spineCount - 1) {
                        currentSpine += 1
                        loadSpine(webView, content, currentSpine, 0f, style, pendingFraction)
                    }
                }
                ReaderCommand.Previous -> {
                    val moved = webView.evalBoolean("window.DogearPager ? DogearPager.prev() : false")
                    if (!moved && currentSpine > 0) {
                        currentSpine -= 1
                        loadSpine(webView, content, currentSpine, 1f, style, pendingFraction)
                    }
                }
                is ReaderCommand.GoTo -> {
                    currentSpine = command.spineIndex
                    loadSpine(webView, content, currentSpine, command.fraction, style, pendingFraction)
                }
            }
        }
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier,
        onRelease = { it.destroyReader() },
    )
}

private suspend fun loadSpine(
    webView: WebView,
    content: BookContent.Reflowable,
    index: Int,
    fraction: Float,
    style: ReflowStyle,
    pendingFraction: androidx.compose.runtime.MutableFloatState,
) {
    pendingFraction.floatValue = fraction
    val doc = withContext(Dispatchers.IO) { content.document(index) }
    val html = ReaderAssets.buildHtml(doc.html, style)
    val base = "https://dogear.local/book/" + if (doc.basePath.isEmpty()) "" else "${doc.basePath}/"
    webView.loadDataWithBaseURL(base, html, "text/html", "utf-8", null)
}

private suspend fun WebView.evalBoolean(js: String): Boolean = suspendCancellableCoroutine { cont ->
    evaluateJavascript(js) { result -> cont.resume(result == "true") }
}

private fun interceptResource(
    content: BookContent.Reflowable,
    request: WebResourceRequest,
): WebResourceResponse {
    val url = request.url
    if (url.host == "dogear.local") {
        val path = url.path?.removePrefix("/book/")?.let { android.net.Uri.decode(it) }
        if (!path.isNullOrEmpty()) {
            val resource = runBlocking(Dispatchers.IO) { content.resource(path) }
            if (resource != null) {
                return WebResourceResponse(resource.mimeType, null, ByteArrayInputStream(resource.bytes))
            }
        }
    }
    // Deny everything else: no network egress, no file access.
    return WebResourceResponse(
        "text/plain", "utf-8", 403, "Blocked",
        emptyMap(), ByteArrayInputStream(ByteArray(0)),
    )
}

private fun WebView.configureForReading() {
    settings.apply {
        javaScriptEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        @Suppress("DEPRECATION")
        allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        allowUniversalAccessFromFileURLs = false
        builtInZoomControls = false
        setSupportZoom(false)
        cacheMode = WebSettings.LOAD_NO_CACHE
        mediaPlaybackRequiresUserGesture = true
    }
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    overScrollMode = View.OVER_SCROLL_NEVER
}

private fun WebView.destroyReader() {
    runCatching { removeJavascriptInterface("DogearBridge") }
    stopLoading()
    (parent as? ViewGroup)?.removeView(this)
    webChromeClient = null
    destroy()
}

/** Receives page-count reports from the injected pager (called on a WebView/JS thread). */
private class PaginationBridge {
    private val _events = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 32)
    val events: SharedFlow<Pair<Int, Int>> = _events.asSharedFlow()

    @JavascriptInterface
    fun onPaginated(count: Int, current: Int) {
        _events.tryEmit(count to current)
    }
}
