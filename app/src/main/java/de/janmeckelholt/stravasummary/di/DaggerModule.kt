package de.janmeckelholt.stravasummary.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.janmeckelholt.stravasummary.PreferencesDataStore
import de.janmeckelholt.stravasummary.data.HealthConnectManager
import de.janmeckelholt.stravasummary.data.HealthConnectRepo
import de.janmeckelholt.stravasummary.data.database.StravaDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaggerModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): PreferencesDataStore {
        return PreferencesDataStore(context)
    }

    @Provides
    @Singleton
    fun provideHealthConnectManager(@ApplicationContext context: Context): HealthConnectManager {
        return HealthConnectManager(context)
    }

    @Provides
    @Singleton
    fun provideRepo(
        healthConnectManager: HealthConnectManager,
        @ApplicationContext context: Context
    ): HealthConnectRepo {
        return HealthConnectRepo(
            healthConnectManager = healthConnectManager,
            dao = StravaDatabase.getInstance(context).stravaDao
        )
    }
}
