package de.janmeckelholt.stravasummary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.ui.composables.DatePickerDocked
import de.janmeckelholt.stravasummary.utils.formatMetersToKm
import de.janmeckelholt.stravasummary.utils.formatSecondsToHMS
import de.janmeckelholt.stravasummary.utils.unixTimeStampToDateString
import de.janmeckelholt.stravasummary.viewmodel.MainViewModel

@Composable
fun WeekSummaryScreen(vm: MainViewModel) {
    val weekSummary by vm.weekSummary.collectAsStateWithLifecycle()
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    Box(modifier = Modifier
        .fillMaxSize(),
        contentAlignment = Alignment.Center) {
        Row() {
            Spacer(modifier = Modifier.weight(2f))
            Column(modifier = Modifier.weight(4f)) {
                Spacer(modifier = Modifier.weight(4f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        DatePickerDocked(vm)
                    }
                    IconButton(onClick = { vm.refreshActivities() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh activities from remote",
                            tint = textColor
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Start of Week: ${unixTimeStampToDateString(weekSummary.startOfWeek)}", color = textColor, fontSize = Constants.FONT_SIZE, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(text = "Number of Runs: ${weekSummary.numberOfRuns}", color = textColor, fontSize = Constants.FONT_SIZE, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(text = "Distance: ${formatMetersToKm(weekSummary.totalDistance)}", color = textColor, fontSize = Constants.FONT_SIZE, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(text = "Moving Time: ${formatSecondsToHMS(weekSummary.totalMovingTime)}", color = textColor, fontSize = Constants.FONT_SIZE, modifier = Modifier.align(Alignment.CenterHorizontally))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .then(
                            if (weekSummary.totalElevationGain > 500) {
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Green)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Text(
                        text = "Elevation Gain: ${weekSummary.totalElevationGain}",
                        color = textColor,
                        fontSize = Constants.FONT_SIZE
                    )
                }
                Text(text = "Number of Others: ${weekSummary.numberOfOthers}", color = textColor, fontSize = Constants.FONT_SIZE, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.weight(4f))
            }
            Spacer(modifier = Modifier.weight(2f))
        }
    }
}