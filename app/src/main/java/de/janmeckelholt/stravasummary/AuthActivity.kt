package de.janmeckelholt.stravasummary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import timber.log.Timber
import androidx.core.net.toUri
import de.janmeckelholt.stravasummary.viewmodel.AuthViewModel
import kotlin.getValue

class AuthActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.let {
            Timber.i("intent: $intent - ${intent.action} - ${intent.categories} ")
            when (it.action) {
                Intent.ACTION_VIEW -> {
                    Timber.i("data: ${it.data}")
                    val codeArray = it.data?.getQueryParameters("code") ?: emptyList()
                    if (codeArray.isEmpty()) {
                        Timber.e("No code found in the intent data")
                        setResult(RESULT_CANCELED)
                        return
                    }
                    val code = it.data?.getQueryParameters("code")?.get(0).toString()
                    Timber.i("___code: $code")

                    authViewModel.saveCode(code)
                    val bundle = Bundle()
                    bundle.putString("code", code)
                    setResult(RESULT_OK, Intent().putExtras(bundle))
                    val intent = Intent(this@AuthActivity, MainActivity::class.java)
                    intent.putExtras(bundle)
                    ContextCompat.startActivity(this@AuthActivity, intent, null)
                    this@AuthActivity.finish()
                }

                else -> {
                    val intentUri = "https://www.strava.com/oauth/mobile/authorize".toUri()
                        .buildUpon()
                        .appendQueryParameter("response_type", Constants.STRAVA_RESPONSE_TYPE)
                        .appendQueryParameter("client_id", BuildConfig.STRAVA_CLIENT_ID)
                        .appendQueryParameter("redirect_uri", Constants.STRAVA_REDIRECT_URI)
                        .appendQueryParameter("approval_prompt", "auto")
                        .appendQueryParameter("scope", Constants.STRAVA_SCOPE)
                        .build()
                    val intent = Intent(Intent.ACTION_VIEW, intentUri)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_FROM_BACKGROUND)
                    Timber.i("Opening Browser...")
                    ContextCompat.startActivity(this@AuthActivity, intent, null)
                    this@AuthActivity.finish()
                }
            }

        }
    }
}