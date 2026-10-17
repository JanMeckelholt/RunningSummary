package de.janmeckelholt.runningsummary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.janmeckelholt.runningsummary.Steps
import de.janmeckelholt.runningsummary.viewmodel.MainViewModel

@Composable
fun PreparationScreen(vm: MainViewModel) {
    val step by vm.step.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Weekly Summary",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                when (step) {
                    Steps.GET_CODE -> {
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Connect your account to see your weekly summaries from Health Connect.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        Button(
                            onClick = { vm.checkPermissionsAndFetch() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Connect with Health Connect")
                        }
                    }

                    Steps.GET_TOKEN, Steps.GET_ATHLETE, Steps.GET_ACTIVITIES -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Authenticating & Fetching Data...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Steps.SHOW_WEEK_SUMMARY -> {}
                    Steps.INITIAL -> {
                        Button(
                            onClick = { vm.checkPermissionsAndFetch() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Start")
                        }
                    }
                }
            }
        }
    }
}
