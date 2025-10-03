package de.janmeckelholt.stravasummary.data.network.models

import de.janmeckelholt.stravasummary.data.database.models.AthleteDto
import kotlinx.serialization.Serializable


@Serializable
data class StravaAthleteResponse(
    val id: Long,
    val username: String?,
    val resource_state: Int?,
    val firstname: String?,
    val lastname: String?,
    val bio: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val sex: String?,
    val premium: Boolean?,
    val summit: Boolean?,
    val created_at: String?,
    val updated_at: String?,
    val badge_type_id: Int?,
    val weight: Double?,
    val profile_medium: String?,
    val profile: String?,
    val friend: String?,
    val follower: String?
) {
    fun toAthleteDto(): AthleteDto {
        return AthleteDto(
            id = id,
            username = username ?: "",
            firstName = firstname,
            lastName = lastname,
        )
    }
}