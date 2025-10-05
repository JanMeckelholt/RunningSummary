package de.janmeckelholt.stravasummary.data.network



import retrofit2.Retrofit
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import de.janmeckelholt.stravasummary.Constants
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

object StravaService {

    val networkJson = Json { ignoreUnknownKeys = true }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.STRAVA_BASE_URL)
            .client(StravaHttpClient.getClient())
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    val api: StravaApi = getRetrofit().create(StravaApi::class.java)
}