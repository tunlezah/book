package com.dogear.reader.format.api.di

import com.dogear.reader.format.api.BookFormatHandler
import dagger.Module
import dagger.multibindings.Multibinds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Declares the multibound set of handlers so the graph is valid even before any format module
 * contributes, and so each module can add its handler with `@Binds @IntoSet`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FormatApiModule {
    @Multibinds
    abstract fun bookFormatHandlers(): Set<BookFormatHandler>
}
