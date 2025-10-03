package de.janmeckelholt.stravasummary.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.data.database.models.AthleteDto
import kotlinx.coroutines.sync.Mutex

@Database(entities = [AthleteDto::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract val stravaDao: StravaDao
    val mutex = Mutex()
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        Constants.STRAVA_DB_NAME
                    ).fallbackToDestructiveMigration(false).build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}