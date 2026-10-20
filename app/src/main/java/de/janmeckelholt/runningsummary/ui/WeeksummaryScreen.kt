package de.janmeckelholt.runningsummary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.janmeckelholt.runningsummary.Constants
import de.janmeckelholt.runningsummary.theme.SuccessGreenContainer
import de.janmeckelholt.runningsummary.theme.SuccessGreenText
import de.janmeckelholt.runningsummary.ui.composables.DatePickerDocked
import de.janmeckelholt.runningsummary.utils.formatMetersToKm
import de.janmeckelholt.runningsummary.utils.formatSecondsToHMS
import de.janmeckelholt.runningsummary.utils.unixTimeStampToDateString
import de.janmeckelholt.runningsummary.viewmodel.MainViewModel

@Composable
fun WeekSummaryScreen(vm: MainViewModel) {
    val weekSummary by vm.weekSummary.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Week Summary",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DatePickerDocked(vm)
            }
            IconButton(onClick = { vm.refreshActivities() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh activities from Health Connect",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                    label = "Start of Week",
                    value = unixTimeStampToDateString(weekSummary.startOfWeek)
                )
                HorizontalDivider()
                SummaryRow(label = "Runs", value = weekSummary.numberOfRuns.toString())
                HorizontalDivider()
                SummaryRow(label = "Distance", value = formatMetersToKm(weekSummary.totalDistance))
                HorizontalDivider()
                SummaryRow(
                    label = "Duration",
                    value = formatSecondsToHMS(weekSummary.totalMovingTime)
                )
                HorizontalDivider()

                val goalReached = weekSummary.totalElevationGain >= Constants.WEEKLY_ELEVATION_GOAL_METERS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Elevation Gain", style = MaterialTheme.typography.bodyLarge)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (goalReached) {
                                    SuccessGreenContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (goalReached) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Goal reached",
                                    tint = SuccessGreenText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "${
                                    kotlin.math.floor(weekSummary.totalElevationGain).toInt()
                                } m",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (goalReached) FontWeight.Bold else FontWeight.Medium,
                                color = if (goalReached) SuccessGreenText else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                HorizontalDivider()
                SummaryRow(
                    label = "Other Activities",
                    value = weekSummary.numberOfOthers.toString()
                )
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
