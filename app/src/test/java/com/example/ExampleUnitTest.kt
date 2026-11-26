package com.example

import com.example.data.Constants
import com.example.data.model.AppItem
import com.example.data.model.GitHubNotificationItem
import com.example.data.model.GitHubNotificationsResponse
import com.example.data.model.StoreResponse
import com.example.ui.notifications.formatDubaiDateTime
import com.example.ui.notifications.formatNotificationTimeShort
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `test app item json parsing and formatting`() {
        val jsonStr = """
        {
          "store": {
            "developerName": "RORAX",
            "developerBio": "Non Profit Organisation",
            "telegram": "https://t.me/my_dad_raaj",
            "email": "rsrobinhossin3@gmail.com",
            "facebook": "https://facebook.com/robinisrorax"
          },
          "apps": [
            {
              "name": "Living Life",
              "package": "com.rorax.note",
              "icon": "https://example.com/icon.png",
              "screenshots": ["https://example.com/s1.png"],
              "description": "Great notes app",
              "versionName": "1.0.1",
              "versionCode": 2,
              "apkUrl": "https://example.com/note.apk",
              "sizeMB": 12.4,
              "updatedAt": "2026-11-06"
            }
          ]
        }
        """.trimIndent()

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

        val response = json.decodeFromString<StoreResponse>(jsonStr)
        assertEquals("RORAX", response.store.developerName)
        assertEquals(1, response.apps.size)

        val app = response.apps.first()
        assertEquals("Living Life", app.name)
        assertEquals("com.rorax.note", app.packageName)
        assertEquals(2L, app.versionCode)
        assertEquals("12.4 MB", app.formattedSize)
        assertEquals("06 Nov 2026", app.formattedUpdatedAt)
    }

    @Test
    fun `test date formatting edge cases`() {
        assertEquals("06 Nov 2026", AppItem.formatDisplayDate("2026-11-06"))
        assertEquals("15 Jan 2025", AppItem.formatDisplayDate("2025-01-15"))
        assertEquals("raw-date", AppItem.formatDisplayDate("raw-date"))
        assertEquals("", AppItem.formatDisplayDate(""))
    }

    @Test
    fun `test actual live github notification json with ISO-8601 offset`() {
        val jsonFeed = """
        {
          "notifications": [
            {
              "id": "2026-11-07-01",
              "type": "announcement",
              "title": "Store maintenance",
              "message": "Downloads may be slow tonight.",
              "package": "",
              "link": "https://t.me/my_dad_raaj",
              "date": "2026-11-07T15:45:00+04:00"
            }
          ]
        }
        """.trimIndent()

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

        val parsed = json.decodeFromString<GitHubNotificationsResponse>(jsonFeed)
        assertEquals(1, parsed.notifications.size)

        val notif = parsed.notifications.first()
        assertEquals("2026-11-07-01", notif.id)
        assertEquals("Store maintenance", notif.title)
        assertEquals("Downloads may be slow tonight.", notif.message)
        assertEquals("https://t.me/my_dad_raaj", notif.link)
        assertEquals("", notif.time)

        val triggerEpoch = notif.calculateTriggerEpochMillis()
        assertNotNull(triggerEpoch)

        // 15:45 at +04:00 corresponds to 11:45 UTC
        val utcFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val utcDateStr = utcFormat.format(triggerEpoch!!)
        assertEquals("2026-11-07 11:45:00", utcDateStr)
    }

    @Test
    fun `test alternative separate date and time format in Dubai time`() {
        val notif = GitHubNotificationItem(
            id = "test-separate",
            title = "Test Separate",
            message = "Body",
            date = "2026-11-07",
            time = "3:20 PM"
        )
        val triggerEpoch = notif.calculateTriggerEpochMillis()
        assertNotNull(triggerEpoch)

        // 3:20 PM Dubai (UTC+4) = 11:20 AM UTC
        val utcFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val utcDateStr = utcFormat.format(triggerEpoch!!)
        assertEquals("2026-11-07 11:20:00", utcDateStr)
    }

    @Test
    fun `test invalid or blank date time parsing returns null safely`() {
        val invalidItem = GitHubNotificationItem(
            id = "test-bad",
            title = "Test",
            message = "Body",
            date = "invalid-date",
            time = "not-a-time"
        )
        assertNull(invalidItem.calculateTriggerEpochMillis())

        val emptyItem = GitHubNotificationItem(
            id = "test-empty",
            title = "Test",
            message = "Body",
            date = "",
            time = ""
        )
        assertNull(emptyItem.calculateTriggerEpochMillis())
    }

    @Test
    fun `test short notification time formatting`() {
        val fixedNow = 1762500000000L
        val past5Min = fixedNow - 5 * 60 * 1000L
        assertEquals("5m ago", formatNotificationTimeShort(past5Min, fixedNow))

        val future30Min = fixedNow + 30 * 60 * 1000L
        assertEquals("In 30m", formatNotificationTimeShort(future30Min, fixedNow))
    }
}
