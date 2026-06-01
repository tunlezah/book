package com.dogear.reader.core.database.di

import android.content.Context
import androidx.room.Room
import com.dogear.reader.core.database.DogearDatabase
import com.dogear.reader.core.database.dao.AnnotationDao
import com.dogear.reader.core.database.dao.BookDao
import com.dogear.reader.core.database.dao.OrganizationDao
import com.dogear.reader.core.database.dao.ReadingProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DogearDatabase =
        Room.databaseBuilder(context, DogearDatabase::class.java, DogearDatabase.NAME)
            // WAL keeps reads non-blocking while the background indexer writes.
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()

    @Provides
    fun provideBookDao(database: DogearDatabase): BookDao = database.bookDao()

    @Provides
    fun provideReadingProgressDao(database: DogearDatabase): ReadingProgressDao =
        database.readingProgressDao()

    @Provides
    fun provideOrganizationDao(database: DogearDatabase): OrganizationDao =
        database.organizationDao()

    @Provides
    fun provideAnnotationDao(database: DogearDatabase): AnnotationDao = database.annotationDao()
}
