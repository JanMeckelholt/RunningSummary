package de.janmeckelholt.stravasummary.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                DatePickerDocked(vm)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Start of Week: ${unixTimeStampToDateString(weekSummary.startOfWeek)}", color = textColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(text = "Number of Runs: ${weekSummary.numberOfRuns}", color = textColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(text = "Distance: ${formatMetersToKm(weekSummary.totalDistance)}", color = textColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(text = "Moving Time: ${formatSecondsToHMS(weekSummary.totalMovingTime)}", color = textColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(text = "Elevation Gain: ${weekSummary.totalElevationGain}", color = textColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(text = "Number of Others: ${weekSummary.numberOfOthers}", color = textColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.weight(4f))
            }
            Spacer(modifier = Modifier.weight(2f))
        }
    }
}