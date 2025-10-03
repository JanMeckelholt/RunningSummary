package de.janmeckelholt.stravasummary.data.network.models

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
