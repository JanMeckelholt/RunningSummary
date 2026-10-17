package de.janmeckelholt.runningsummary.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.janmeckelholt.runningsummary.utils.convertMillisToDateStr
import de.janmeckelholt.runningsummary.viewmodel.MainViewModel
import timber.log.Timber
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDocked(vm: MainViewModel) {
    val displayedDate by vm.displayedDate.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val selectedDate = convertMillisToDateStr(displayedDate * 1000)

    Box() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            OutlinedTextField(
                value = selectedDate,
                onValueChange = {},
                label = { Text(selectedDate) },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select date"
                    )
                },
                interactionSource = remember { MutableInteractionSource() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .zIndex(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showDatePicker = true }
            )
        }

        if (showDatePicker) {
            Popup(
                onDismissRequest = { showDatePicker = false },
                alignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 64.dp)
                        .shadow(elevation = 4.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false
                    )
                    if (datePickerState.selectedDateMillis != null) {
                        LaunchedEffect(datePickerState.selectedDateMillis) {
                            val selectedMillis = datePickerState.selectedDateMillis!!
                            val selectedLocalDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            val mondayLocalDate =
                                selectedLocalDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                            val mondaySeconds =
                                mondayLocalDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
                            vm.setDisplayedDate(mondaySeconds)
                            showDatePicker = false
                            Timber.i("___ Selected millis: $selectedMillis -> Monday (local) seconds: $mondaySeconds")
                            datePickerState.selectedDateMillis = null
                        }
                    }
                }
            }
        }
    }
}
