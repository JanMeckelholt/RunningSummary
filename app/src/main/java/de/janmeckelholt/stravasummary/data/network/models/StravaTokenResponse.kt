package de.janmeckelholt.stravasummary.data.network.models

import kotlinx.serialization.Serializable

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
data class StravaTokenRequestWithCodeBody(
    val client_id: String,
    val client_secret: String,
    val code: String,
    val grant_type: String
)

@Serializable
data class StravaRefreshTokenRequestBody(
    val client_id: String,
    val client_secret: String,
    val refresh_token: String,
    val grant_type: String
)