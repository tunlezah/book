package com.dogear.reader.feature.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Distraction-free system UI for the reader: hides the status/navigation bars (sticky immersive)
 * and keeps the screen awake while reading, restoring both when the reader leaves composition.
 * Uses [WindowInsetsControllerCompat] per the Reader Engineering Research note; controls that are
 * shown apply their own insets, so nothing is double-padded.
 */
@Composable
internal fun ReaderSystemUi(immersive: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return

    DisposableEffect(immersive) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (immersive) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
