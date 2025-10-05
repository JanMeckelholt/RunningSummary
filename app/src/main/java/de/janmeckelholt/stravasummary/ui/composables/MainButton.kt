package de.janmeckelholt.stravasummary.ui.composables

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MainButton(btnText: String, onButtonClick: () -> Unit) {
    // This is a simple button that can be used in the IdleScreen or anywhere else
    Button(onClick = onButtonClick) {
        Text(text = btnText)
    }
}