package com.dogear.reader.format.fb2.di

import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.fb2.Fb2FormatHandler
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class Fb2Module {
    @Binds
    @IntoSet
    abstract fun bindFb2Handler(handler: Fb2FormatHandler): BookFormatHandler
}
