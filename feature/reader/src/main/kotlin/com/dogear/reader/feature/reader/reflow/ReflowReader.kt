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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
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

/** Virtual origin for serving in-book resources via [WebViewAssetLoader]. */
private const val ASSET_DOMAIN = "appassets.androidplatform.net"

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
    smoothPaging: Boolean,
    commands: SharedFlow<ReaderCommand>,
    onProgress: (Locator) -> Unit,
    onHighlight: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var spineCount by remember { mutableIntStateOf(1) }
    var currentSpine by remember { mutableIntStateOf(initialLocator?.spineIndex ?: 0) }
    val pendingFraction = remember { mutableFloatStateOf(0f) }
    // Last known position within the current document, so a style change re-anchors here.
    val currentFraction = remember { mutableFloatStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }
    val bridge = remember { PaginationBridge() }

    // Serve the book's resources over a virtual https origin with the official asset loader.
    // It returns a response only for our /book/ paths and null for everything else (including the
    // inline main document), which is exactly what avoids the ERR_HTTP_RESPONSE_CODE_FAILURE trap.
    val assetLoader = remember(content) {
        WebViewAssetLoader.Builder()
            .setDomain(ASSET_DOMAIN)
            .addPathHandler("/book/") { path ->
                val resource = runBlocking(Dispatchers.IO) { runCatching { content.resource(path) }.getOrNull() }
                if (resource != null) {
                    WebResourceResponse(resource.mimeType, null, ByteArrayInputStream(resource.bytes))
                } else {
                    // Known origin but missing entry: empty 200 so the page doesn't network-fault.
                    WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
            }
            .build()
    }

    val webView = remember {
        WebView(context).apply {
            configureForReading()
            addJavascriptInterface(bridge, "DogearBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = interceptResource(assetLoader, request)

                override fun onPageFinished(view: WebView, url: String?) {
                    // Position restore is handled inside the pager once fonts/images settle and the
                    // layout is stable (see ReaderAssets) — restoring here would race the reflow and
                    // land on a blank offset. We only nudge a recompute as a late safety net.
                    view.evaluateJavascript("window.DogearPager && DogearPager.recompute();", null)
                }
            }
        }
    }

    // Bridge reports page counts after each layout/move → persist a DOM-stable locator.
    LaunchedEffect(bridge) {
        bridge.events.collect { (count, current) ->
            val docFraction = if (count <= 1) 0f else current.toFloat() / (count - 1)
            currentFraction.floatValue = docFraction
            val overall = if (spineCount <= 0) 0f else (currentSpine + docFraction) / spineCount
            onProgress(Locator(spineIndex = currentSpine, progression = overall.coerceIn(0f, 1f)))
        }
    }

    // Initial load + live re-render when typography/theme/animation change, re-anchoring position.
    LaunchedEffect(style, smoothPaging) {
        spineCount = content.spine().size.coerceAtLeast(1)
        val fraction = if (!initialized) {
            initialized = true
            initialLocator?.let { (it.progression * spineCount - currentSpine).coerceIn(0f, 1f) } ?: 0f
        } else {
            currentFraction.floatValue
        }
        loadSpine(webView, content, currentSpine, fraction, style, smoothPaging, pendingFraction)
    }

    LaunchedEffect(commands) {
        commands.collect { command ->
            when (command) {
                ReaderCommand.Next -> {
                    val moved = webView.evalBoolean("window.DogearPager ? DogearPager.next() : false")
                    if (!moved && currentSpine < spineCount - 1) {
                        currentSpine += 1
                        loadSpine(webView, content, currentSpine, 0f, style, smoothPaging, pendingFraction)
                    }
                }
                ReaderCommand.Previous -> {
                    val moved = webView.evalBoolean("window.DogearPager ? DogearPager.prev() : false")
                    if (!moved && currentSpine > 0) {
                        currentSpine -= 1
                        loadSpine(webView, content, currentSpine, 1f, style, smoothPaging, pendingFraction)
                    }
                }
                is ReaderCommand.GoTo -> {
                    currentSpine = command.spineIndex
                    loadSpine(webView, content, currentSpine, command.fraction, style, smoothPaging, pendingFraction)
                }
                is ReaderCommand.GoToProgression -> {
                    val target = (command.progression * spineCount).toInt().coerceIn(0, spineCount - 1)
                    val fraction = (command.progression * spineCount - target).coerceIn(0f, 1f)
                    currentSpine = target
                    loadSpine(webView, content, currentSpine, fraction, style, smoothPaging, pendingFraction)
                }
                ReaderCommand.RequestHighlight -> {
                    val raw = webView.evalString("(window.getSelection?window.getSelection().toString():'')")
                    val text = raw.trim()
                    if (text.isNotEmpty()) onHighlight(text)
                }
            }
        }
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.destroyReader() },
    )
}

private suspend fun loadSpine(
    webView: WebView,
    content: BookContent.Reflowable,
    index: Int,
    fraction: Float,
    style: ReflowStyle,
    smoothPaging: Boolean,
    pendingFraction: androidx.compose.runtime.MutableFloatState,
) {
    pendingFraction.floatValue = fraction
    val doc = withContext(Dispatchers.IO) { content.document(index) }
    val html = ReaderAssets.buildHtml(doc.html, style, smoothPaging, fraction)
    // Base URL points at the asset-loader origin so relative resource refs resolve to /book/<path>.
    val base = "https://$ASSET_DOMAIN/book/" + if (doc.basePath.isEmpty()) "" else "${doc.basePath}/"
    webView.loadDataWithBaseURL(base, html, "text/html", "utf-8", null)
}

private suspend fun WebView.evalBoolean(js: String): Boolean = suspendCancellableCoroutine { cont ->
    evaluateJavascript(js) { result -> cont.resume(result == "true") }
}

private suspend fun WebView.evalString(js: String): String = suspendCancellableCoroutine { cont ->
    evaluateJavascript(js) { result ->
        // evaluateJavascript returns a JSON-encoded string, e.g. "\"hello\"".
        val decoded = result
            ?.removeSurrounding("\"")
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
            ?: ""
        cont.resume(if (decoded == "null") "" else decoded)
    }
}

private fun interceptResource(
    assetLoader: WebViewAssetLoader,
    request: WebResourceRequest,
): WebResourceResponse? {
    // In-book resources are served by the asset loader (returns null for anything it doesn't own).
    assetLoader.shouldInterceptRequest(request.url)?.let { return it }
    // Block external network egress silently (empty 200 — never an error status, which would
    // fault the frame). Inline (data:) main-document and other schemes pass through (null).
    val scheme = request.url.scheme?.lowercase()
    if (scheme == "http" || scheme == "https") {
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
    }
    return null
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
