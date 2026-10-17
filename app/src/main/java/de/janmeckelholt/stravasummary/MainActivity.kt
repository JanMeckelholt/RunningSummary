package de.janmeckelholt.stravasummary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.janmeckelholt.stravasummary.data.HealthConnectManager
import de.janmeckelholt.stravasummary.theme.RunningTheme
import de.janmeckelholt.stravasummary.ui.PreparationScreen
import de.janmeckelholt.stravasummary.ui.WeekSummaryScreen
import de.janmeckelholt.stravasummary.viewmodel.MainViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var healthConnectManager: HealthConnectManager

    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        Timber.i("Health Connect permission result granted: $granted")
        Timber.i("Required permissions: ${healthConnectManager.permissions}")
        if (granted.containsAll(healthConnectManager.permissions)) {
            mainViewModel.fetchActivities()
        } else {
            Timber.e("Health Connect permissions denied. Granted: $granted, Needed: ${healthConnectManager.permissions}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            RunningTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MainScreen(
                            vm = mainViewModel,
                            onRequestPermissions = {
                                val status = healthConnectManager.getSdkStatus()
                                Timber.i("HealthConnect SDK Status: $status (AVAILABLE=${HealthConnectClient.SDK_AVAILABLE})")
                                requestPermissions.launch(healthConnectManager.permissions)
                            }
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            if (healthConnectManager.hasAllPermissions()) {
                mainViewModel.fetchActivities()
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel, onRequestPermissions: () -> Unit) {
    val step by vm.step.collectAsStateWithLifecycle()

    LaunchedEffect(step) {
        if (step == Steps.GET_CODE) { // We hijacked GET_CODE for asking permissions
            onRequestPermissions()
            vm.setStep(Steps.GET_TOKEN) // Move to loading step so we don't endlessly launch intent
        }
    }

    when (step) {
        Steps.INITIAL, Steps.GET_CODE, Steps.GET_TOKEN, Steps.GET_ATHLETE, Steps.GET_ACTIVITIES -> {
            PreparationScreen(vm)
        }
        Steps.SHOW_WEEK_SUMMARY -> {
            WeekSummaryScreen(vm)
        }
    }
}
