package com.dogear.reader.core.cover

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.absoluteValue

/**
 * Draws a deterministic typographic cover from title + author when a book has no embedded
 * image. The background color is derived from the title hash so it is stable across rebuilds
 * (Cover Management: "looks intentional, never broken").
 */
internal object GeneratedCover {

    fun create(title: String, author: String, width: Int = 400, height: Int = 600): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val bg = backgroundColor(title)
        canvas.drawColor(bg)

        val margin = width * 0.1f
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = width * 0.11f
        }
        val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = width * 0.06f
        }

        val textWidth = (width - 2 * margin).toInt().coerceAtLeast(1)
        val titleLayout = buildLayout(title.take(80), titlePaint, textWidth)
        val authorLayout = buildLayout(author.take(60), authorPaint, textWidth)

        canvas.save()
        canvas.translate(margin, height * 0.18f)
        titleLayout.draw(canvas)
        canvas.restore()

        canvas.save()
        canvas.translate(margin, height * 0.78f)
        authorLayout.draw(canvas)
        canvas.restore()

        return bitmap
    }

    private fun buildLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .setMaxLines(5)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

    private fun backgroundColor(seed: String): Int {
        val palette = intArrayOf(
            0xFF4E6E5D.toInt(), 0xFF5D5E8C.toInt(), 0xFF8C5D5D.toInt(), 0xFF8C7A5D.toInt(),
            0xFF5D7A8C.toInt(), 0xFF6E5D8C.toInt(), 0xFF3A4A5A.toInt(), 0xFF7A5D6E.toInt(),
            0xFF4A5D3A.toInt(), 0xFF8C6E4E.toInt(),
        )
        return palette[seed.hashCode().absoluteValue % palette.size]
    }
}
