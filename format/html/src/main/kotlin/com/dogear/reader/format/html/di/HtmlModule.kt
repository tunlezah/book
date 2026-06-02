package com.dogear.reader.format.html.di

import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.html.HtmlFormatHandler
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HtmlModule {
    @Binds
    @IntoSet
    abstract fun bindHtmlHandler(handler: HtmlFormatHandler): BookFormatHandler
}
