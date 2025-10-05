package de.janmeckelholt.stravasummary.data.network

import de.janmeckelholt.stravasummary.data.network.models.StravaActivity
import de.janmeckelholt.stravasummary.data.network.models.StravaAthlete
import de.janmeckelholt.stravasummary.data.network.models.StravaRefreshTokenRequestBody
import de.janmeckelholt.stravasummary.data.network.models.StravaTokenRequestWithCodeBody
import de.janmeckelholt.stravasummary.data.network.models.StravaTokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StravaApi {
    @POST("api/v3/oauth/token")
    suspend fun getTokenWithCode(
        @Body body: StravaTokenRequestWithCodeBody,
    ): StravaTokenResponse

    @POST("api/v3/oauth/token")
    suspend fun refreshToken(
        @Body body: StravaRefreshTokenRequestBody,
    ): StravaTokenResponse

    @GET("/api/v3/athlete")
    suspend fun getAthlete(
    ): StravaAthlete

    @GET("api/v3/athlete/activities")
    suspend fun getActivities(
        @Query("before") before: Long,
        @Query("after") after: Long
    ): List<StravaActivity>
}