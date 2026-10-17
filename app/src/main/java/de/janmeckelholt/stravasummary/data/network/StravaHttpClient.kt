package de.janmeckelholt.stravasummary.data.network

import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.PreferencesDataStore
import de.janmeckelholt.stravasummary.TimberApplication
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

class StravaHttpClient : OkHttpClient() {
    companion object {
        fun getClient(): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val httpResponseInterceptor = HttpResponseInterceptor(handleExpectedFailures = { response, originalRequest ->
                when (response.code) {
                    401 -> {
                        Timber.i("401 from StravaHttpClient - probably token expired. Trying to re-authenticate...")
                        runBlocking {
                            PreferencesDataStore(TimberApplication.appContext).saveString(key = Constants.STRAVA_ACCESS_TOKEN_KEY, value = "")
                            Credentials.refreshAccessToken(reset = true)
                            // Return request with new ctxDa-token
                            return@runBlocking originalRequest.newBuilder().header("Authorization", "Bearer ${Credentials.updateAccessToken() ?: ""}").build()
                        }
                    }
                    else -> {
                        Timber.w("Unexpected response code ${response.code} from StravaHttpClient")
                    }
                }
                null
            })

            return Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val request = original
                        .newBuilder()
                        .addHeader("accept", "application/json")
                        .addHeader("Authorization", "Bearer ${Credentials.accessToken ?: ""}")
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(httpResponseInterceptor)
                .build()
        }
    }
}