package de.janmeckelholt.stravasummary.viewmodel

sealed class MainState {

    object Idle: MainState()
    object Loading: MainState()
    data class SuccessGettingToken(val token: String): MainState()
    data class SuccessGettingAthlete(val firstName: String): MainState()
    data class Error(val error: String?): MainState()

}