package de.janmeckelholt.stravasummary.models

import kotlinx.serialization.Serializable

data class TokenRequest(
    val client_id: String = "",
    val redirect_uri: String = "",
    val response_type: String = "code",
    val approval_prompt: String = "auto",
    val scope: String = "activity:write,read"
)


@Serializable
data class StravaTokenResponse(
    val token_type: String = "",
    val expires_at: Long? = null,
    val expires_in: Long? =null,
    val refresh_token: String? = null,
    val access_token: String = "",
    //val athlete: Any? = null
)

@Serializable
data class StravaTokenRequestBody(
    val client_id: String,
    val client_secret: String,
    val code: String,
    val grant_type: String
)

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
)