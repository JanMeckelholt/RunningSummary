package de.janmeckelholt.stravasummary.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.data.database.models.AthleteDto
import kotlinx.coroutines.flow.Flow

@Dao
interface StravaDao {
    @Transaction
    @Query("select * from ${Constants.STRAVA_ATHLETE_TABLE_NAME}")
    fun getAthleteLiveData(): Flow<List<AthleteDto>?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllAthletes(vararg athletes: AthleteDto)
}
