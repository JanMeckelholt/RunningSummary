package de.janmeckelholt.runningsummary.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.janmeckelholt.runningsummary.Constants
import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import de.janmeckelholt.runningsummary.data.database.models.AthleteDto
import kotlinx.coroutines.sync.Mutex

@Database(entities = [AthleteDto::class, ActivityDto::class], version = 2, exportSchema = false)
abstract class RunningDatabase : RoomDatabase() {
    abstract val runningDao: RunningDao
    val mutex = Mutex()

    companion object {
        @Volatile
        private var INSTANCE: RunningDatabase? = null
        fun getInstance(context: Context): RunningDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        RunningDatabase::class.java,
                        Constants.RUNNING_DB_NAME
                    ).fallbackToDestructiveMigration(true).build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
