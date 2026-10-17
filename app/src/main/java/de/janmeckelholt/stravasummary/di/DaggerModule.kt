package de.janmeckelholt.stravasummary.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.PreferencesDataStore
import de.janmeckelholt.stravasummary.data.StravaRepo
import de.janmeckelholt.stravasummary.data.database.StravaDatabase
import de.janmeckelholt.stravasummary.data.network.AuthManager
import de.janmeckelholt.stravasummary.data.network.HttpResponseInterceptor
import de.janmeckelholt.stravasummary.data.network.StravaApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaggerModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): PreferencesDataStore {
        return PreferencesDataStore(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authManager: AuthManager,
        dataStore: PreferencesDataStore
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val httpResponseInterceptor =
            HttpResponseInterceptor(handleExpectedFailures = { response, originalRequest ->
                when (response.code) {
                    401 -> {
                        Timber.i("401 from StravaHttpClient - probably token expired. Trying to re-authenticate...")
                        runBlocking {
                            dataStore.saveString(
                                key = Constants.STRAVA_ACCESS_TOKEN_KEY,
                                value = ""
                            )
                            authManager.refreshAccessToken(reset = true)
                        }
                        // Return request with new token
                        val newToken = authManager.getAccessTokenBlocking()
                        return@HttpResponseInterceptor originalRequest.newBuilder()
                            .header("Authorization", "Bearer ${newToken ?: ""}")
                            .build()
                    }

                    else -> {
                        Timber.w("Unexpected response code ${response.code} from StravaHttpClient")
                    }
                }
                null
            })

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original
                    .newBuilder()
                    .addHeader("accept", "application/json")
                    .addHeader("Authorization", "Bearer ${authManager.accessToken ?: ""}")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(httpResponseInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideStravaApi(okHttpClient: OkHttpClient): StravaApi {
        val networkJson = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(Constants.STRAVA_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(StravaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRepo(api: StravaApi, @ApplicationContext context: Context): StravaRepo {
        return StravaRepo(api = api, dao = StravaDatabase.getInstance(context).stravaDao)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): androidx.work.WorkManager {
        return androidx.work.WorkManager.getInstance(context)
    }
}
