package de.janmeckelholt.stravasummary

import androidx.compose.ui.unit.sp

object Constants {
    const val STRAVA_BASE_URL  = "https://www.strava.com/"
    const val STRAVA_GRANT_TYPE_CODE = "authorization_code"
    const val STRAVA_GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
    const val STRAVA_RESPONSE_TYPE = "code"
    const val STRAVA_REDIRECT_URI = "intent://running_app/redirect"
    const val STRAVA_SCOPE = "activity:read"
    const val PREFERENCES_DATA_STORE_NAME = "preferencesDataStore"
    const val STRAVA_ACCESS_TOKEN_KEY = "stravaAccessTokenKey"
    const val STRAVA_REFRESH_TOKEN_KEY = "stravaRefreshTokenKey"
    const val STRAVA_CODE_KEY = "staravaCodeKey"
    const val STRAVA_ATHLETE_ID_KEY = "staravaAthleteIdKey"
    const val STRAVA_DB_NAME = "strava_db"
    const val STRAVA_ATHLETE_TABLE_NAME = "strava_athlete_table"
    const val STRAVA_ACTIVITY_TABLE_NAME = "strava_activity_table"
    const val OLDEST_SYNCED_TIMESTAMP_KEY = "oldestSyncedTimestampKey"
    const val BACKFILL_COMPLETED_KEY = "backfillCompletedKey"
    val FONT_SIZE = 16.sp
}

enum class Steps{
    INITIAL,
    GET_CODE,
    GET_TOKEN,
    GET_ATHLETE,
    GET_ACTIVITIES,
    SHOW_WEEK_SUMMARY


}