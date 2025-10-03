package de.janmeckelholt.stravasummary

object Constants {
    const val STRAVA_ClIENT_ID  = BuildConfig.STRAVA_CLIENT_ID
    const val STRAVA_CLIENT_SECRET = BuildConfig.STRAVA_CLIENT_SECRET
    const val STRAVA_GRANT_TYPE = "authorization_code"
    const val STRAVA_RESPONSE_TYPE = "code"
    const val STRAVA_REDIRECT_URI = "intent://running_app/redirect"
    const val STRAVA_SCOPE = "activity:read"
    const val PREFERENCES_DATA_STORE_NAME = "preferencesDataStore"
    const val STRAVA_TOKEN_KEY = "staravaTokenKey"
    const val STRAVA_CODE_KEY = "staravaCodeKey"
}