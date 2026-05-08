package com.ekenahmetfaruk.vigna_scan.di

import android.content.Context
import com.ekenahmetfaruk.vigna_scan.ml.ModelManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideModelManager(
        @ApplicationContext context: Context
    ): ModelManager = ModelManager(context)
}