package de.janmeckelholt.runningsummary

import android.app.Application
import timber.log.Timber

open class TimberApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
