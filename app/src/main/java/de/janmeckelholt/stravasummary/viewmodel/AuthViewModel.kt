package de.janmeckelholt.stravasummary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.janmeckelholt.stravasummary.data.network.AuthManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    fun saveCode(code: String) {
        viewModelScope.launch {
            authManager.updateCode(code)
        }
    }
}