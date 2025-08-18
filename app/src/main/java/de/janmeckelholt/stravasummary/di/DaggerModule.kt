package de.janmeckelholt.stravasummary.di

import de.janmeckelholt.stravasummary.api.StravaRepo
import de.janmeckelholt.stravasummary.api.StravaService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaggerModule {

    @Provides
    @Singleton
    fun provideRepo(): StravaRepo {
        return StravaRepo(api = StravaService.api)
    }
}