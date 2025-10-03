package de.janmeckelholt.stravasummary.data.network

import de.janmeckelholt.stravasummary.data.network.models.Activity
import de.janmeckelholt.stravasummary.data.network.models.StravaAthleteResponse
import de.janmeckelholt.stravasummary.data.network.models.StravaTokenRequestBody
import de.janmeckelholt.stravasummary.data.network.models.StravaTokenResponse
import de.janmeckelholt.stravasummary.utils.unixTimeStampToNextSundayMidnight
import de.janmeckelholt.stravasummary.utils.unixTimeStampToPreviousMondayMorning
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface StravaApi {
//    @GET("oauth/mobile/authorize")
//    suspend fun XXXgetToken(
//        @Query("client_id") clientId: Long = Constants.STRAVA_ClIENT_ID,
//        @Query("redirect_uri") redirect_uri: String = Constants.STRAVA_REDIRECT_URI,
//        @Query("response_type") response_type: String = "code",
//        @Query("approval_prompt") approval_prompt: String = "auto",
//        @Query("scope") scope: String = Constants.STRAVA_SCOPE
//    ): StravaTokenResponse

    @POST("api/v3/oauth/token")
    suspend fun getToken(
        @Body body: StravaTokenRequestBody,
    ): StravaTokenResponse

    @GET("/api/v3/athlete")
    suspend fun getAthlete(
        @Header("Authorization") accessToken: String
    ): StravaAthleteResponse
    //http https://www.strava.com/api/v3/athlete 'Authorization: Bearer 83ebeabdec09f6670863766f792ead24d61fe3f9'


    @GET("api/v3/athlete/activities")
    suspend fun getActivities(
        @Header("Authorization") accessToken: String,
        @Path("before") before: Long,
        @Path("after") after: Long
    ): List<Activity>

    @GET("api/v3/athlete/activities")
    suspend fun getWeekActivities(
        unixTimeStamp: Long,
        @Header("Authorization") accessToken: String,
        @Path("before") before: Long = unixTimeStampToNextSundayMidnight(unixTimeStamp),
        @Path("after") after: Long = unixTimeStampToPreviousMondayMorning(unixTimeStamp)
    ): List<Activity>
}