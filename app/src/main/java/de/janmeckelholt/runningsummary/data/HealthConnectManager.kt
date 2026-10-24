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
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(@param:ApplicationContext private val context: Context) {
    val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    fun getSdkStatus(): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    val basicPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class)
    )

    val allPermissions =
        basicPermissions + setOf(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY)

    suspend fun getGrantedPermissions(): Set<String> {
        if (getSdkStatus() != HealthConnectClient.SDK_AVAILABLE) {
            return emptySet()
        }
        return try {
            healthConnectClient.permissionController.getGrantedPermissions()
        } catch (_: Exception) {
            emptySet()
        }
    }

    suspend fun hasBasicPermissions(): Boolean {
        return getGrantedPermissions().containsAll(basicPermissions)
    }

    suspend fun hasHistoryPermission(): Boolean {
        return getGrantedPermissions().contains(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY)
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
