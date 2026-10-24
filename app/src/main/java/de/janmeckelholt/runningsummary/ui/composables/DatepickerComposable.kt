package de.janmeckelholt.runningsummary.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.janmeckelholt.runningsummary.utils.unixTimeStampToDateString
import de.janmeckelholt.runningsummary.utils.unixTimeStampToPreviousMondayMorning
import de.janmeckelholt.runningsummary.viewmodel.MainViewModel
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDocked(vm: MainViewModel) {
    val displayedDate by vm.displayedDate.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val displayedUtcMillis = remember(displayedDate) {
        Instant.ofEpochSecond(displayedDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = displayedUtcMillis
    )
    val selectedDateText = unixTimeStampToDateString(displayedDate)

    Box {
        OutlinedTextField(
            value = selectedDateText,
            onValueChange = {},
            label = { Text("Week starting") },
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
                ) {
                    datePickerState.selectedDateMillis = displayedUtcMillis
                    showDatePicker = true
                }
        )

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            val localSeconds =
                                selectedLocalDate.atStartOfDay(ZoneId.systemDefault())
                                    .toEpochSecond()
                            val mondaySeconds = unixTimeStampToPreviousMondayMorning(localSeconds)
                            vm.setDisplayedDate(mondaySeconds)
                            Timber.i("___ Selected millis: $millis -> Monday seconds: $mondaySeconds")
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false
                )
            }
        }
    }
}
