package de.janmeckelholt.stravasummary.data.database.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.janmeckelholt.stravasummary.Constants
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Entity(tableName = Constants.STRAVA_ACTIVITY_TABLE_NAME)
@Serializable
data class ActivityDto(
    @PrimaryKey
    val id: Long,
    val athleteId: Long?,
    val name: String?,
    val distance: Double?,
    val movingTime: Int?,
    val elapsedTime: Int?,
    val totalElevationGain: Double?,
    val type: String?,
    val startDate: String?,
    val startDateLocal: String?,
    val startDateUnix: Long?,
    val timezone: String?,
    val utcOffset: Double?,
    val sportType: String?,
) : Parcelable