package de.janmeckelholt.runningsummary.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant

@Singleton
class HealthConnectManager @Inject constructor(@ApplicationContext private val context: Context) {
    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    fun getSdkStatus(): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    val healthPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class)
    )

    val permissions = healthPermissions + setOf("android.permission.health.READ_HEALTH_DATA_HISTORY")

    suspend fun hasBasicPermissions(): Boolean {
        if (getSdkStatus() != HealthConnectClient.SDK_AVAILABLE) {
            return false
        }
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(healthPermissions)
    }

    suspend fun hasAllPermissions(): Boolean {
        val hasHealthPermissions = hasBasicPermissions()
        val hasHistoryPermission = if (android.os.Build.VERSION.SDK_INT >= 34) {
            context.checkSelfPermission("android.permission.health.READ_HEALTH_DATA_HISTORY") == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return hasHealthPermissions && hasHistoryPermission
    }

    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseSessionRecord> {
        val allRecords = mutableListOf<ExerciseSessionRecord>()
        var pageToken: String? = null
        do {
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                pageToken = pageToken
            )
            val response = healthConnectClient.readRecords(request)
            allRecords.addAll(response.records)
            pageToken = response.pageToken
        } while (!pageToken.isNullOrEmpty())
        return allRecords
    }

    suspend fun readDistanceRecords(start: Instant, end: Instant): List<DistanceRecord> {
        val allRecords = mutableListOf<DistanceRecord>()
        var pageToken: String? = null
        do {
            val request = ReadRecordsRequest(
                recordType = DistanceRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                pageToken = pageToken
            )
            val response = healthConnectClient.readRecords(request)
            allRecords.addAll(response.records)
            pageToken = response.pageToken
        } while (!pageToken.isNullOrEmpty())
        return allRecords
    }

    suspend fun readElevationGainedRecords(
        start: Instant,
        endInstant: Instant
    ): List<ElevationGainedRecord> {
        val allElevation = mutableListOf<ElevationGainedRecord>()
        var pageToken: String? = null
        do {
            val request = ReadRecordsRequest(
                recordType = ElevationGainedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, endInstant),
                pageToken = pageToken
            )
            val response = healthConnectClient.readRecords(request)
            allElevation.addAll(response.records)
            pageToken = response.pageToken
        } while (!pageToken.isNullOrEmpty())
        return allElevation
    }
}
