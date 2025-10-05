package de.janmeckelholt.stravasummary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.janmeckelholt.stravasummary.BuildConfig
import de.janmeckelholt.stravasummary.Steps
import de.janmeckelholt.stravasummary.data.network.Credentials
import de.janmeckelholt.stravasummary.ui.composables.MainButton
import de.janmeckelholt.stravasummary.viewmodel.MainViewModel

@Composable
fun PreparationScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val displayedDate by vm.displayedDate.collectAsStateWithLifecycle()
    val formattedDisplayedDate by vm.formattedDisplayedDate.collectAsStateWithLifecycle()
    val athleteDto by vm.athleteDto.collectAsStateWithLifecycle()
    val step by vm.step.collectAsStateWithLifecycle()
    key(step) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column {
                Text(text = "Strava-Client-ID: ${BuildConfig.STRAVA_CLIENT_ID}")
                Text(text = "Date-Week: $formattedDisplayedDate ($displayedDate)")
                Text(text = "Strava Code: ${Credentials.code}")
                Text(text = "Strava Token: ${Credentials.accessToken}")
                Text(text = "Athlete: ${athleteDto?.firstName ?: "No Athlete"} ${athleteDto?.lastName ?: ""}")
                when (step) {
                    Steps.GET_CODE -> MainButton(btnText = "Get Strava Code", onButtonClick = { vm.getStravaCode(context) })
                    Steps.GET_TOKEN -> MainButton(btnText = "Get Strava Token", onButtonClick = { vm.getAccessToken() })
                    Steps.GET_ATHLETE -> MainButton(btnText = "Get Strava Athlete", onButtonClick = { vm.fetchAthlete() })
                    Steps.GET_ACTIVITIES -> MainButton(btnText = "Get Strava Activities", onButtonClick = { vm.fetchActivities() })
                    Steps.SHOW_WEEK_SUMMARY -> {}
                    Steps.INITIAL -> {}
                }
            }

        }
    }

}