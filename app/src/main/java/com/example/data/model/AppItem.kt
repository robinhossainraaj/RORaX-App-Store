package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppItem(
    val name: String = "",
    @SerialName("package") val packageName: String = "",
    val icon: String = "",
    val screenshots: List<String> = emptyList(),
    val description: String = "",
    val versionName: String = "",
    val versionCode: Long = 0,
    val apkUrl: String = "",
    val sizeMB: Double = 0.0,
    val updatedAt: String = ""
) {
    val formattedSize: String
        get() = if (sizeMB > 0) "%.1f MB".format(sizeMB) else ""

    val formattedUpdatedAt: String
        get() = formatDisplayDate(updatedAt)

    companion object {
        fun formatDisplayDate(dateStr: String): String {
            if (dateStr.isBlank()) return ""
            // Expected input format e.g. "2026-11-06"
            return try {
                val parts = dateStr.split("-")
                if (parts.size == 3) {
                    val year = parts[0]
                    val month = parts[1].toIntOrNull() ?: return dateStr
                    val day = parts[2]
                    val monthName = when (month) {
                        1 -> "Jan"
                        2 -> "Feb"
                        3 -> "Mar"
                        4 -> "Apr"
                        5 -> "May"
                        6 -> "Jun"
                        7 -> "Jul"
                        8 -> "Aug"
                        9 -> "Sep"
                        10 -> "Oct"
                        11 -> "Nov"
                        12 -> "Dec"
                        else -> return dateStr
                    }
                    "$day $monthName $year"
                } else {
                    dateStr
                }
            } catch (e: Exception) {
                dateStr
            }
        }
    }
}
