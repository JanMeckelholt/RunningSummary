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
    @Transaction
    @Query("select * from ${Constants.ACTIVITY_TABLE_NAME} where startDateUnix <= :before and startDateUnix >= :after order by startDateUnix desc")
    fun getActivitiesLiveData(before: Long, after: Long): Flow<List<ActivityDto>?>

    @Transaction
    @Query("select * from ${Constants.ACTIVITY_TABLE_NAME} where startDateUnix <= :before and startDateUnix >= :after order by startDateUnix desc")
    fun getActivities(before: Long, after: Long): List<ActivityDto>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllActivities(vararg activities: ActivityDto)
}
