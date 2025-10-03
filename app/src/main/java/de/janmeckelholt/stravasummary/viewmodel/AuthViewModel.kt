package de.janmeckelholt.stravasummary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.PreferencesDataStore
import de.janmeckelholt.stravasummary.TimberApplication
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {
    fun saveCode(code: String) {
        viewModelScope.launch {
            PreferencesDataStore(TimberApplication.appContext).saveString(key= Constants.STRAVA_CODE_KEY, value= code)
        }

    }
}