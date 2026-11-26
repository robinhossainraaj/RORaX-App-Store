package com.example.data.model

import com.example.data.Constants
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.TimeZone

@Serializable
data class GitHubNotificationsResponse(
    val notifications: List<GitHubNotificationItem> = emptyList()
)

@Serializable
data class GitHubNotificationItem(
    val id: String = "",
    val type: String = "announcement",
    val title: String = "",
    val message: String = "",
    val `package`: String = "",
    val link: String = "",
    val date: String = "", // e.g. "2026-11-07T15:45:00+04:00" or "2026-11-07"
    val time: String = "", // e.g. "3:20 PM", "15:45", or empty if included in date
    val versionCode: Long? = null
) {
    /**
     * Calculates the notification's trigger timestamp in UTC epoch milliseconds.
     * Supports:
     * 1. ISO-8601 with explicit offset: e.g. "2026-11-07T15:45:00+04:00" or "2026-11-07T15:45:00Z"
     * 2. ISO-8601 without offset: e.g. "2026-11-07T15:45:00" (evaluated in Asia/Dubai)
     * 3. Separate date and 12-hour/24-hour time: e.g. date="2026-11-07", time="3:20 PM" (evaluated in Asia/Dubai)
     * 4. Date and time combined with space: e.g. date="2026-11-07 15:45:00" (evaluated in Asia/Dubai)
     * 5. Date only: e.g. date="2026-11-07" (evaluated in Asia/Dubai)
     */
    fun calculateTriggerEpochMillis(): Long? {
        val cleanDate = date.trim()
        if (cleanDate.isBlank()) return null
        val cleanTime = time.trim()
            .replace("\u00A0", " ")
            .uppercase(Locale.US)

        val dubaiZone = ZoneId.of(Constants.DUBAI_ZONE_ID)
        val dubaiTz = TimeZone.getTimeZone(Constants.DUBAI_ZONE_ID)

        // 1. Try Java 8 Time APIs if ISO 'T' is present
        if (cleanDate.contains("T")) {
            // Check for explicit offset (+, -, or Z)
            try {
                return OffsetDateTime.parse(cleanDate).toInstant().toEpochMilli()
            } catch (_: Exception) {}

            try {
                return ZonedDateTime.parse(cleanDate).toInstant().toEpochMilli()
            } catch (_: Exception) {}

            try {
                // If no timezone in string, interpret in Asia/Dubai
                return LocalDateTime.parse(cleanDate).atZone(dubaiZone).toInstant().toEpochMilli()
            } catch (_: Exception) {}
        }

        // 2. Prepare combined date-time string
        val combined = if (cleanTime.isNotBlank()) {
            "$cleanDate $cleanTime"
        } else {
            cleanDate
        }

        // 3. Fallback SimpleDateFormat patterns
        val candidatePatterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd h:mm a",
            "yyyy-MM-dd hh:mm a",
            "yyyy-MM-dd H:mm",
            "yyyy-MM-dd"
        )

        for (pattern in candidatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = dubaiTz
                    isLenient = false
                }
                val pos = java.text.ParsePosition(0)
                val parsed = sdf.parse(combined, pos)
                if (parsed != null && pos.index == combined.length) {
                    return parsed.time
                }
            } catch (_: Exception) {}
        }

        return null
    }
}

/**
 * Entity persisted locally to track scheduled alarms and metadata.
 */
@Serializable
data class ScheduledNotificationEntity(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val packageName: String,
    val link: String,
    val triggerEpochMillis: Long,
    val rawDate: String,
    val rawTime: String
)
