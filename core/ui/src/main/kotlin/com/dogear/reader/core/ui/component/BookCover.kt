package com.dogear.reader.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kotlin.math.absoluteValue

/**
 * Renders a book cover. Real covers stream from the on-disk cache via Coil (off the main
 * thread, so scrolling never blocks). When a book has no cover, a deterministic typographic
 * cover is drawn from the title/author — it looks intentional, never broken.
 */
@Composable
fun BookCover(
    title: String,
    author: String,
    coverPath: String?,
    isGenerated: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(modifier = modifier.clip(shape)) {
        if (coverPath != null && !isGenerated) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(coverPath))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            GeneratedCover(title = title, author = author, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun GeneratedCover(title: String, author: String, modifier: Modifier) {
    val base = generatedColor(title)
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(base, base.copy(alpha = 0.78f))),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = author,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Deterministic, pleasant cover color from the title so re-renders are stable. */
private fun generatedColor(seed: String): Color {
    val palette = listOf(
        0xFF4E6E5D, 0xFF5D5E8C, 0xFF8C5D5D, 0xFF8C7A5D, 0xFF5D7A8C,
        0xFF6E5D8C, 0xFF3A4A5A, 0xFF7A5D6E, 0xFF4A5D3A, 0xFF8C6E4E,
    )
    val index = (seed.hashCode().absoluteValue) % palette.size
    return Color(palette[index])
}
