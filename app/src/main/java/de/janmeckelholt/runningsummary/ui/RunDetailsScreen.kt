package de.janmeckelholt.runningsummary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.janmeckelholt.runningsummary.utils.formatMetersToKm
import de.janmeckelholt.runningsummary.utils.formatPacePerKm
import de.janmeckelholt.runningsummary.utils.formatSecondsToHMS
import de.janmeckelholt.runningsummary.utils.unixTimeStampToDateString
import de.janmeckelholt.runningsummary.utils.unixTimeStampToDateTimeString
import de.janmeckelholt.runningsummary.utils.unixTimeStampToShortDayDateString
import de.janmeckelholt.runningsummary.viewmodel.MainViewModel
import kotlin.math.floor

@Composable
fun RunDetailsScreen(vm: MainViewModel) {
    val weekSummary by vm.weekSummary.collectAsStateWithLifecycle()
    val selectedRunId by vm.selectedRunId.collectAsStateWithLifecycle()

    BackHandler {
        vm.closeRunDetails()
    }

    val runs = weekSummary.runs
    val selectedRun = runs.find { it.id == selectedRunId } ?: runs.lastOrNull()
    val listState = rememberLazyListState()

    LaunchedEffect(selectedRun?.id, runs.size) {
        val selectedIndex = runs.indexOfFirst { it.id == selectedRun?.id }
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.closeRunDetails() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Week Summary",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Run Details",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.size(48.dp))
        }

        Text(
            text = "Week of ${unixTimeStampToDateString(weekSummary.startOfWeek)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (runs.isEmpty() || selectedRun == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No runs recorded for this week.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(runs, key = { it.id }) { run ->
                    val isSelected = run.id == selectedRun.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { vm.selectRun(run.id) },
                        label = {
                            Text(
                                text = "${unixTimeStampToShortDayDateString(run.startDateUnix)} • ${formatMetersToKm(run.distance)}",
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryRow(
                        label = "Run",
                        value = selectedRun.name
                    )
                    HorizontalDivider()
                    SummaryRow(
                        label = "Date & Time",
                        value = unixTimeStampToDateTimeString(selectedRun.startDateUnix)
                    )
                    HorizontalDivider()
                    SummaryRow(
                        label = "Distance",
                        value = formatMetersToKm(selectedRun.distance)
                    )
                    HorizontalDivider()
                    SummaryRow(
                        label = "Duration",
                        value = formatSecondsToHMS(selectedRun.movingTime)
                    )
                    HorizontalDivider()
                    SummaryRow(
                        label = "Average Pace",
                        value = formatPacePerKm(selectedRun.distance, selectedRun.movingTime)
                    )
                    HorizontalDivider()
                    SummaryRow(
                        label = "Elevation Gain",
                        value = "${floor(selectedRun.totalElevationGain).toInt()} m"
                    )
                }
            }
        }
    }
}

