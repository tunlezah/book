package com.dogear.reader.feature.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dogear.reader.core.model.BrightnessMode
import com.dogear.reader.core.model.KeepAwakeMode
import com.dogear.reader.core.model.OrientationLock
import com.dogear.reader.core.model.ReaderSettings

/**
 * Distraction-free system UI for the reader: sticky-immersive system bars, plus live application
 * of the brightness / keep-awake / orientation controls (Phase 4, Screen Management). Everything
 * is restored when the reader leaves composition.
 */
@Composable
internal fun ReaderSystemUi(settings: ReaderSettings) {
    val activity = LocalContext.current.findActivity() ?: return

    // Immersive bars are independent of the screen-control settings.
    DisposableEffect(Unit) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(
        settings.brightnessMode,
        settings.brightnessLevel,
        settings.keepAwake,
        settings.orientation,
    ) {
        val window = activity.window

        // Brightness: READER overrides the window; SYSTEM/AUTO defer to the OS.
        val attrs = window.attributes
        attrs.screenBrightness = when (settings.brightnessMode) {
            BrightnessMode.READER -> settings.brightnessLevel.coerceIn(0.01f, 1f)
            BrightnessMode.SYSTEM, BrightnessMode.AUTO ->
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window.attributes = attrs

        // Keep awake.
        val keepOn = when (settings.keepAwake) {
            KeepAwakeMode.NEVER -> false
            KeepAwakeMode.WHILE_READING -> true
            KeepAwakeMode.WHILE_CHARGING -> activity.isCharging()
        }
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Orientation lock.
        activity.requestedOrientation = when (settings.orientation) {
            OrientationLock.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        onDispose {
            val restore = window.attributes
            restore.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = restore
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

private fun Activity.isCharging(): Boolean {
    val status = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val plugged = status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    return plugged != 0
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
