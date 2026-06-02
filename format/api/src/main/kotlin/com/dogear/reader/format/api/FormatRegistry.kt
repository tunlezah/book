package com.dogear.reader.format.api

import com.dogear.reader.core.model.BookFormat
import javax.inject.Inject

/**
 * Resolves a [FileRef] to the handler that can read it. Handlers are contributed via Hilt
 * multibinding from each format module, so adding a format is purely additive — no change here.
 */
class FormatRegistry @Inject constructor(
    handlers: Set<@JvmSuppressWildcards BookFormatHandler>,
) {
    // Highest priority first so signature-based handlers beat the text catch-all.
    private val ordered = handlers.sortedByDescending { it.priority }

    /** Probes handlers in priority order, returning the first match with its format, or null. */
    suspend fun resolve(ref: FileRef): Resolution? {
        for (handler in ordered) {
            val result = runCatching { handler.probe(ref) }.getOrDefault(ProbeResult.NoMatch)
            if (result.matches) return Resolution(handler, result.format)
        }
        return null
    }

    fun handlerFor(format: BookFormat): BookFormatHandler? =
        ordered.firstOrNull { format in it.supportedFormats }

    data class Resolution(val handler: BookFormatHandler, val format: BookFormat)
}
