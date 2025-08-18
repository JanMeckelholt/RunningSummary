package de.janmeckelholt.stravasummary.viewmodel

import de.janmeckelholt.stravasummary.MainActivity

sealed class MainIntent {

    class GetStravaCode(val ctx: MainActivity): MainIntent()
    class FetchStravaToken(): MainIntent()
    class FetchAthlete(): MainIntent()

}