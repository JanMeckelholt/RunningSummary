package de.janmeckelholt.stravasummary.di

import de.janmeckelholt.stravasummary.data.StravaRepo
import de.janmeckelholt.stravasummary.data.network.StravaService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.janmeckelholt.stravasummary.TimberApplication
import de.janmeckelholt.stravasummary.data.database.StravaDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaggerModule {

    @Provides
    @Singleton
    fun provideRepo(): StravaRepo {
        return StravaRepo(api = StravaService.api, dao = StravaDatabase.getInstance(TimberApplication.appContext).stravaDao)
    }
}