package de.janmeckelholt.runningsummary.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import de.janmeckelholt.runningsummary.Constants
import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningDao {
    @Query("SELECT * FROM ${Constants.ACTIVITY_TABLE_NAME} WHERE startDateUnix <= :before AND startDateUnix >= :after ORDER BY startDateUnix DESC")
    fun getActivitiesLiveData(before: Long, after: Long): Flow<List<ActivityDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllActivities(vararg activities: ActivityDto)

    @Query("DELETE FROM ${Constants.ACTIVITY_TABLE_NAME} WHERE startDateUnix <= :before AND startDateUnix >= :after")
    suspend fun deleteActivities(before: Long, after: Long)

    @Transaction
    suspend fun replaceActivitiesInRange(before: Long, after: Long, activities: List<ActivityDto>) {
        deleteActivities(before = before, after = after)
        if (activities.isNotEmpty()) {
            insertAllActivities(*activities.toTypedArray())
        }
    }
}
