package com.dogear.reader.format.text.di

import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.text.TxtFormatHandler
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TextModule {
    @Binds
    @IntoSet
    abstract fun bindTxtHandler(handler: TxtFormatHandler): BookFormatHandler
}
