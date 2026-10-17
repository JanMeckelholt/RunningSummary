package de.janmeckelholt.stravasummary

import android.content.Intent
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.janmeckelholt.stravasummary.theme.RunningTheme
import de.janmeckelholt.stravasummary.ui.LoadingScreen
import de.janmeckelholt.stravasummary.ui.PreparationScreen
import de.janmeckelholt.stravasummary.ui.WeekSummaryScreen
import de.janmeckelholt.stravasummary.viewmodel.MainViewModel
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
                mainViewModel.saveCodeAndResume(code)
            }
        }
        enableEdgeToEdge()
        setContent {
            RunningTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MainScreen(vm = mainViewModel)
                    }
                }
            }
        }

        lifecycleScope.launch {
            mainViewModel.navigateToAuth.collect {
                try {
                    val authIntent = Intent(this@MainActivity, AuthActivity::class.java)
                    startActivity(authIntent)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val step by vm.step.collectAsStateWithLifecycle()
    when (step) {
        Steps.INITIAL -> LoadingScreen()
        Steps.GET_CODE, Steps.GET_TOKEN, Steps.GET_ATHLETE, Steps.GET_ACTIVITIES -> PreparationScreen(vm)
        Steps.SHOW_WEEK_SUMMARY -> {
            vm.fetchActivities()
            WeekSummaryScreen(vm)
        }
    }

}






