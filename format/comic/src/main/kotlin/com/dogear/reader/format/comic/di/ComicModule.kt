package com.dogear.reader.format.comic.di

import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.comic.CbzFormatHandler
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ComicModule {
    @Binds
    @IntoSet
    abstract fun bindCbzHandler(handler: CbzFormatHandler): BookFormatHandler
}
