package de.janmeckelholt.runningsummary

object Constants {
    const val RUNNING_DB_NAME = "runningsummary"
    const val ACTIVITY_TABLE_NAME = "activity"

    // PreferencesDataStore keys
    const val LAST_SYNC_EPOCH_KEY = "last_sync_epoch"

    // Default lookback: 1 year in seconds
    const val DEFAULT_BACKFILL_SECONDS = 365L * 24 * 60 * 60

    // Weekly elevation goal in meters
    const val WEEKLY_ELEVATION_GOAL_METERS = 500.0
}
