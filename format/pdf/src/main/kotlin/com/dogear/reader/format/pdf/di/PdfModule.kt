package com.dogear.reader.format.pdf.di

import com.dogear.reader.format.api.BookFormatHandler
import com.dogear.reader.format.pdf.PdfFormatHandler
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfModule {
    @Binds
    @IntoSet
    abstract fun bindPdfHandler(handler: PdfFormatHandler): BookFormatHandler
}
