package de.janmeckelholt.stravasummary.ui

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
import de.janmeckelholt.stravasummary.Steps
import de.janmeckelholt.stravasummary.viewmodel.MainViewModel

@Composable
fun PreparationScreen(vm: MainViewModel) {
    val athleteDto by vm.athleteDto.collectAsStateWithLifecycle()
    val step by vm.step.collectAsStateWithLifecycle()

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
                    text = "Strava Summary",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (athleteDto != null) {
                    Text(
                        text = "Hello, ${athleteDto?.firstName}!",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                when (step) {
                    Steps.GET_CODE -> {
                        Text(
                            text = "Connect your account to see your weekly summaries.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { vm.getStravaCode() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Connect with Strava")
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
                    Steps.INITIAL -> {}
                }
            }
        }
    }
}