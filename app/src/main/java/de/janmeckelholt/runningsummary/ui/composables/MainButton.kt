package de.janmeckelholt.runningsummary.ui.composables

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MainButton(btnText: String, onButtonClick: () -> Unit) {
    Button(onClick = onButtonClick) {
        Text(text = btnText)
    }
}
