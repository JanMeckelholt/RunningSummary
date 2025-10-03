package de.janmeckelholt.stravasummary.data.database.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.janmeckelholt.stravasummary.Constants
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Entity(tableName = Constants.STRAVA_ATHLETE_TABLE_NAME)
@Serializable
data class AthleteDto(
    @PrimaryKey
    val id: Long,
    val username: String,
    val firstName: String?,
    val lastName: String?,
) : Parcelable