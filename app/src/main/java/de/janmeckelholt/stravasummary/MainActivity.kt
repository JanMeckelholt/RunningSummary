package de.janmeckelholt.stravasummary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.janmeckelholt.stravasummary.theme.RunningTheme
import de.janmeckelholt.stravasummary.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import de.janmeckelholt.stravasummary.data.network.Credentials
import de.janmeckelholt.stravasummary.utils.formatMetersToKm
import de.janmeckelholt.stravasummary.utils.formatSecondsToHMS
import de.janmeckelholt.stravasummary.utils.unixTimeStampToDateString
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let { intent ->
            intent.extras?.getString("code")?.let {
                val code = it
                Timber.i("Received code: $code")
                Credentials.updateCode(code)
                mainViewModel.setStep(Steps.GET_TOKEN)
            }
        }
        enableEdgeToEdge()
        setContent {
            RunningTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(vm = mainViewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val step by vm.step.collectAsStateWithLifecycle()
    val displayedDate by vm.displayedDate.collectAsStateWithLifecycle()
    val formattedDisplayedDate by vm.formattedDisplayedDate.collectAsStateWithLifecycle()
    val weekActivities by vm.weekActivities.collectAsStateWithLifecycle()
    val athleteDto by vm.athleteDto.collectAsStateWithLifecycle()
    if (step == Steps.INITIAL) {
        LoadingScreen()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column {
                Text(text = "Strava-Client-ID: ${BuildConfig.STRAVA_CLIENT_ID}")
                Text(text = "Date-Week: $formattedDisplayedDate ($displayedDate)")
                Text(text = "Strava Code: ${Credentials.code}")
                Text(text = "Strava Token: ${Credentials.accessToken}")
                Text(text = "Athlete: ${athleteDto?.firstName ?: "No Athlete"} ${athleteDto?.lastName ?: ""}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Activities this week: ")
                    if (weekActivities.isEmpty()) {
                        Text(text = "None")
                    } else {
                        Button(onClick = { vm.showWeekOf(epoch = displayedDate) }) {
                            Text(text = "${weekActivities.size}")
                        }
                    }
                }
                when (step) {
                    Steps.GET_CODE -> MainButton(btnText = "Get Strava Code", onButtonClick = { vm.getStravaCode(context) })
                    Steps.GET_TOKEN -> MainButton(btnText = "Get Strava Token", onButtonClick = { vm.getAccessToken() })
                    Steps.GET_ATHLETE -> MainButton(btnText = "Get Strava Athlete", onButtonClick = { vm.fetchAthlete() })
                    Steps.GET_ACTIVITIES -> MainButton(btnText = "Get Strava Activities", onButtonClick = { vm.fetchActivities() })
                    Steps.OBSERVE_ACTIVITIES -> {}
                    Steps.SHOW_WEEK_SUMMARY -> WeekSummary(vm = vm)
                    Steps.INITIAL -> {}
                }
            }

        }
    }
}


@Composable
fun MainButton(btnText: String, onButtonClick: () -> Unit) {
    // This is a simple button that can be used in the IdleScreen or anywhere else
    Button(onClick = onButtonClick) {
        Text(text = btnText)
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
        Column {
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Loading...", color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.weight(1f))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                color = Color.White,
            )
            Spacer(modifier = Modifier.weight(1f))
        }

    }

}

@Composable
fun WeekSummary(vm: MainViewModel) {
    val weekSummary by vm.weekSummary.collectAsStateWithLifecycle()

    Column {
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "Start of Week: ${unixTimeStampToDateString(weekSummary.startOfWeek)}", color = Color.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(text = "Number of Runs: ${weekSummary.numberOfRuns}", color = Color.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(text = "Distance: ${formatMetersToKm(weekSummary.totalDistance)}", color = Color.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(text = "Moving Time: ${formatSecondsToHMS(weekSummary.totalMovingTime)}", color = Color.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(text = "Elevation Gain: ${weekSummary.totalElevationGain}", color = Color.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(text = "Number of Others: ${weekSummary.numberOfOthers}", color = Color.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.weight(1f))
    }
}