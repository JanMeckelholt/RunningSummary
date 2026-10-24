package de.janmeckelholt.runningsummary

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.janmeckelholt.runningsummary.data.HealthConnectManager
import de.janmeckelholt.runningsummary.theme.RunningTheme
import de.janmeckelholt.runningsummary.ui.PreparationScreen
import de.janmeckelholt.runningsummary.ui.RunDetailsScreen
import de.janmeckelholt.runningsummary.ui.WeekSummaryScreen
import de.janmeckelholt.runningsummary.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var healthConnectManager: HealthConnectManager

    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        Timber.i("Health Connect permission result granted: $granted")
        lifecycleScope.launch {
            if (healthConnectManager.hasBasicPermissions()) {
                mainViewModel.fetchActivities()
            } else {
                Timber.e("Health Connect basic permissions denied. Granted: $granted")
                mainViewModel.setErrorMessage("Health Connect permissions were denied. Please grant permissions to view your summary.")
                mainViewModel.setStep(Steps.REQUESTING_PERMISSIONS)
            }
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
                                when (healthConnectManager.getSdkStatus()) {
                                    HealthConnectClient.SDK_AVAILABLE -> {
                                        mainViewModel.setStep(Steps.LOADING)
                                        requestPermissions.launch(healthConnectManager.allPermissions)
                                    }

                                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                                        mainViewModel.setErrorMessage("Health Connect update required. Please update Health Connect from the Play Store.")
                                        mainViewModel.setStep(Steps.REQUESTING_PERMISSIONS)
                                    }

                                    else -> {
                                        mainViewModel.setErrorMessage("Health Connect is not available on this device.")
                                        mainViewModel.setStep(Steps.REQUESTING_PERMISSIONS)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            if (healthConnectManager.hasBasicPermissions()) {
                mainViewModel.fetchActivities()
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel, onRequestPermissions: () -> Unit) {
    val step by vm.step.collectAsStateWithLifecycle()

    when (step) {
        Steps.INITIAL, Steps.REQUESTING_PERMISSIONS, Steps.LOADING -> {
            PreparationScreen(vm, onRequestPermissions)
        }

        Steps.SHOWING_SUMMARY -> {
            WeekSummaryScreen(vm)
        }

        Steps.SHOWING_RUN_DETAILS -> {
            RunDetailsScreen(vm)
        }
    }
}
