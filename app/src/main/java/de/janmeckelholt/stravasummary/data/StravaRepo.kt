package de.janmeckelholt.stravasummary.data

import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.data.network.StravaApi
import de.janmeckelholt.stravasummary.data.network.models.StravaAthleteResponse
import de.janmeckelholt.stravasummary.data.network.models.StravaTokenRequestBody
import de.janmeckelholt.stravasummary.data.network.models.StravaTokenResponse

class StravaRepo(private val api: StravaApi) {
    suspend fun getToken(code: String): StravaTokenResponse = api.getToken(
        body = StravaTokenRequestBody(
            client_id = Constants.STRAVA_ClIENT_ID,
            client_secret = Constants.STRAVA_CLIENT_SECRET,
            code = code,
            grant_type = Constants.STRAVA_GRANT_TYPE
        )
    )

    suspend fun getAthlete(token: String): StravaAthleteResponse = api.getAthlete("Bearer $token")

}