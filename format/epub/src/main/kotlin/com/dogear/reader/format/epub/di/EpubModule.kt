package com.dogear.reader.format.epub.di

import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.epub.EpubFormatHandler
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EpubModule {
    @Binds
    @IntoSet
    abstract fun bindEpubHandler(handler: EpubFormatHandler): BookFormatHandler
}
