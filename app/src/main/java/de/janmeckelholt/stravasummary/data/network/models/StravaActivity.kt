package de.janmeckelholt.stravasummary.data.network.models

import de.janmeckelholt.stravasummary.data.database.models.ActivityDto
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class StravaActivity(
    val resource_state: Int? = null,
    val athlete: StravaAthlete? = null,
    val name: String? = null,
    val distance: Double? = null,
    val moving_time: Int? = null,
    val elapsed_time: Int? = null,
    val total_elevation_gain: Double? = null,
    val type: String? = null,
    val sport_type: String? = null,
    val workout_type: Int? = null,
    val id: Long? = null,
    val start_date: String? = null,
    val start_date_local: String? = null,
    val timezone: String? = null,
    val utc_offset: Double? = null,
    val location_city: String? = null,
    val location_state: String? = null,
    val location_country: String? = null,
    val achievement_count: Int? = null,
    val kudos_count: Int? = null,
    val comment_count: Int? = null,
    val athlete_count: Int? = null,
    val photo_count: Int? = null,
    val map: MapData? = null,
    val trainer: Boolean? = null,
    val commute: Boolean? = null,
    val manual: Boolean? = null,
    val private: Boolean? = null,
    val visibility: String? = null,
    val flagged: Boolean? = null,
    val gear_id: String? = null,
    val start_latlng: List<Double>? = null,
    val end_latlng: List<Double>? = null,
    val average_speed: Double? = null,
    val max_speed: Double? = null,
    val average_cadence: Double? = null,
    val average_temp: Int? = null,
    val has_heartrate: Boolean? = null,
    val average_heartrate: Double? = null,
    val max_heartrate: Double? = null,
    val heartrate_opt_out: Boolean? = null,
    val display_hide_heartrate_option: Boolean? = null,
    val elev_high: Double? = null,
    val elev_low: Double? = null,
    val upload_id: Long? = null,
    val upload_id_str: String? = null,
    val external_id: String? = null,
    val from_accepted_tag: Boolean? = null,
    val pr_count: Int? = null,
    val total_photo_count: Int? = null,
    val has_kudoed: Boolean? = null
) {
    fun toActivityDto(): ActivityDto {
        val startDateUnix = start_date?.let {
            try {
                Instant.parse(it).epochSecond
            } catch (e: Exception) {
                null
            }
        }
        return ActivityDto(
            id = id ?: 0,
            athleteId = athlete?.id,
            name = name,
            distance = distance,
            movingTime = moving_time,
            elapsedTime = elapsed_time,
            totalElevationGain = total_elevation_gain,
            type = type,
            startDate = start_date,
            startDateLocal = start_date_local,
            startDateUnix = startDateUnix,
            timezone = timezone,
            utcOffset = utc_offset,
            sportType = sport_type
        )
    }
}
@Serializable
data class MapData(
    val id: String? = null,
    val summary_polyline: String? = null,
    val resource_state: Int? = null
)
