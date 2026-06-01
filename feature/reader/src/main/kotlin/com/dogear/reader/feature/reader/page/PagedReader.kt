package com.dogear.reader.feature.reader.page

import android.graphics.Bitmap
import android.util.Size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.dogear.reader.core.model.Locator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/**
 * Fixed-page (PDF) and image (CBZ) renderer. A [HorizontalPager] turns pages; each page decodes
 * its bitmap on demand at viewport resolution and only when near the viewport, so memory stays
 * bounded (Reader Engineering Research §2–3). PDF page access is serialized inside the content
 * object, so concurrent pager prefetch is safe.
 */
@Composable
internal fun PagedReader(
    pageCount: Int,
    initialLocator: Locator?,
    renderPage: suspend (index: Int, target: Size) -> Bitmap?,
    onProgress: (Locator) -> Unit,
    pagerState: PagerState = rememberPagerState(
        initialPage = initialLocator?.pageIndex
            ?: ((initialLocator?.progression ?: 0f) * (pageCount - 1).coerceAtLeast(0)).toInt(),
        pageCount = { pageCount },
    ),
) {
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val progression = if (pageCount <= 1) 0f else page.toFloat() / (pageCount - 1)
                onProgress(Locator(pageIndex = page, progression = progression))
            }
    }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        PageImage(page = page, renderPage = renderPage)
    }
}

@Composable
private fun PageImage(page: Int, renderPage: suspend (Int, Size) -> Bitmap?) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val target = remember(configuration) {
        with(density) {
            Size(
                configuration.screenWidthDp.dp.toPx().toInt().coerceAtLeast(1),
                configuration.screenHeightDp.dp.toPx().toInt().coerceAtLeast(1),
            )
        }
    }

    val bitmap by produceState<Bitmap?>(initialValue = null, page, target) {
        value = withContext(Dispatchers.Default) { renderPage(page, target) }
    }

    // Recycle the page bitmap when it scrolls away.
    DisposableEffect(bitmap) {
        onDispose { bitmap?.recycle() }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Page ${page + 1}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        )
    }
}
