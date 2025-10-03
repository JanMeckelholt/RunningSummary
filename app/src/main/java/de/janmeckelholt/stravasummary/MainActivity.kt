package de.janmeckelholt.stravasummary

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import de.janmeckelholt.stravasummary.theme.RunningTheme
import de.janmeckelholt.stravasummary.viewmodel.MainIntent
import de.janmeckelholt.stravasummary.viewmodel.MainState
import de.janmeckelholt.stravasummary.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
                mainViewModel.setStravaCode(code)
            }
        }

        val onButtonClickGetCode: () -> Unit = {
            lifecycleScope.launch {
                mainViewModel.userIntent.send(MainIntent.GetStravaCode(ctx = this@MainActivity))
            }
        }

        val onButtonClickGetToken: () -> Unit = {
            lifecycleScope.launch {
                mainViewModel.userIntent.send(MainIntent.FetchStravaToken())
            }
        }



        enableEdgeToEdge()
        setContent {
            RunningTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        vm = mainViewModel,
                        onButtonClickGetCode = onButtonClickGetCode,
                        onButtonClickGetToken = onButtonClickGetToken
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel, onButtonClickGetCode: () -> Unit, onButtonClickGetToken: () -> Unit) {
    val state = vm.state.value

    when (state) {
        is MainState.Idle -> IdleScreen(vm = vm, onButtonClickGetCode = onButtonClickGetCode, onButtonClickGetToken = onButtonClickGetToken)
        is MainState.Loading -> LoadingScreen()
        is MainState.SuccessGettingAthlete -> Success(text = state.firstName)

        is MainState.Error -> {
            IdleScreen(vm = vm, onButtonClickGetCode = onButtonClickGetCode, onButtonClickGetToken = onButtonClickGetToken)
            Timber.i("Error: ${state.error}")
            Toast.makeText(LocalContext.current, state.error, Toast.LENGTH_SHORT).show()
        }

        is MainState.SuccessGettingToken -> {
            ShowStravaToken(vm = vm, token = state.token)
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
fun IdleScreen(vm: MainViewModel, onButtonClickGetCode: () -> Unit, onButtonClickGetToken: () -> Unit) {
    val code by vm.stravaCode.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            if (code.isNullOrEmpty()) MainButton(btnText = "Get Strava Code", onButtonClick = onButtonClickGetCode)
            else {
                Text(text = "Strava Code: $code")
                MainButton(btnText = "Get Strava Token", onButtonClick = onButtonClickGetToken)
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun Success(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            Text(text = "Success Text: $text")
        }
    }
}

@Composable
fun ShowStravaToken(vm: MainViewModel, token: String) {
    val onButtonClick: () -> Unit = {
        vm.viewModelScope.launch {
            vm.userIntent.send(MainIntent.FetchAthlete())
        }

    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            Text(text = "Strava Token: $token")
            MainButton(btnText = "Get Strava Athlete", onButtonClick = onButtonClick)
        }

    }
}