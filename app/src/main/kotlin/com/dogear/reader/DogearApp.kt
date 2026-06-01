package com.dogear.reader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Kept intentionally light — no blocking work in onCreate, per the
 * startup performance budget. Hilt wires the dependency graph.
 */
@HiltAndroidApp
class DogearApp : Application()
