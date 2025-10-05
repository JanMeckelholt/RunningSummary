package de.janmeckelholt.stravasummary.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.data.database.models.ActivityDto
import de.janmeckelholt.stravasummary.data.database.models.AthleteDto
import kotlinx.coroutines.flow.Flow

@Dao
interface StravaDao {
    @Transaction
    @Query("select * from ${Constants.STRAVA_ATHLETE_TABLE_NAME} where id = :id")
    fun getAthleteLiveData(id: Long): Flow<AthleteDto>?

    @Transaction
    @Query("select * from ${Constants.STRAVA_ATHLETE_TABLE_NAME} where id = :id")
    fun getAthleteById(id: Long): AthleteDto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllAthletes(vararg athletes: AthleteDto)

    @Transaction
    @Query("select * from ${Constants.STRAVA_ACTIVITY_TABLE_NAME} where startDateUnix <= :before and startDateUnix >= :after order by startDateUnix desc")
    fun getActivitiesLiveData(before: Long, after: Long): Flow<List<ActivityDto>?>

    @Transaction
    @Query("select * from ${Constants.STRAVA_ACTIVITY_TABLE_NAME} where startDateUnix <= :before and startDateUnix >= :after order by startDateUnix desc")
    fun getActivities(before: Long, after: Long): List<ActivityDto>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllActivities(vararg activities: ActivityDto)
}
