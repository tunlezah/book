package com.dogear.reader.core.common.dispatchers

import javax.inject.Qualifier

/**
 * Injected dispatchers keep work off the main thread and make code testable: tests swap in a
 * deterministic [kotlinx.coroutines.test.TestDispatcher]. Never hard-code Dispatchers.* in
 * repositories or use cases.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
