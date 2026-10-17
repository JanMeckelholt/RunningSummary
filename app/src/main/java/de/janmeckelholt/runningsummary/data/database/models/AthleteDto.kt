package de.janmeckelholt.runningsummary.data.database.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.janmeckelholt.runningsummary.Constants
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Entity(tableName = Constants.ATHLETE_TABLE_NAME)
@Serializable
data class AthleteDto(
    @PrimaryKey
    val id: Long,
    val username: String,
    val firstName: String?,
    val lastName: String?,
) : Parcelable
