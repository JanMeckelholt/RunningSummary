package de.janmeckelholt.runningsummary.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.janmeckelholt.runningsummary.PreferencesDataStore
import de.janmeckelholt.runningsummary.data.HealthConnectManager
import de.janmeckelholt.runningsummary.data.HealthConnectRepo
import de.janmeckelholt.runningsummary.data.database.RunningDatabase
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
            dao = RunningDatabase.getInstance(context).runningDao
        )
    }
}
