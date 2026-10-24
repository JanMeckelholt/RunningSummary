package de.janmeckelholt.runningsummary.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import de.janmeckelholt.runningsummary.data.database.models.ActivityDto

@Database(entities = [ActivityDto::class], version = 4, exportSchema = false)
abstract class RunningDatabase : RoomDatabase() {
    abstract val runningDao: RunningDao
}
