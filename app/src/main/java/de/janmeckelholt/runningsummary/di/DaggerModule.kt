package de.janmeckelholt.runningsummary.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.janmeckelholt.runningsummary.Constants
import de.janmeckelholt.runningsummary.PreferencesDataStore
import de.janmeckelholt.runningsummary.data.database.RunningDao
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
    fun provideRunningDatabase(@ApplicationContext context: Context): RunningDatabase {
        return Room.databaseBuilder(
            context,
            RunningDatabase::class.java,
            Constants.RUNNING_DB_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    fun provideRunningDao(database: RunningDatabase): RunningDao {
        return database.runningDao
    }
}
