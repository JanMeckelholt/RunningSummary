package de.janmeckelholt.stravasummary


import android.app.Application
import android.content.Context
import timber.log.Timber

open class TimberApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TimberApplication.appContext = applicationContext
         Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                super.log(priority, "strava_summary_log", message, t)
            }
            override fun createStackElementTag(element: StackTraceElement): String {
                val maxLength = 68
                val res = String.format(
                    "[C:%s] [M:%s] [L:%s]",
                    super.createStackElementTag(element),
                    element.methodName,
                    element.lineNumber,
                )
                if (res.length > maxLength) return res.substring(0, maxLength - 1)
                return res
            }
        })

    }
    companion object {
        lateinit var appContext: Context
    }
}