# RORAX App Store — GitHub-only scheduled notifications (AI implementation guide)

## Goal

Modify my **existing Android app** to read notifications from my public GitHub JSON database and show **local Android notifications at the date and time written in that JSON**. Do not add Firebase, Firebase Cloud Messaging (FCM), OneSignal, or any other push-notification provider. Do not rebuild the app from scratch.

The GitHub repository is the notification database only. The Android app must download the JSON, parse it, and schedule a local notification on each device.

## Notification database (use this exact URL)

`https://raw.githubusercontent.com/robinhossainraaj/App-Store-/refs/heads/main/Notifications/get_notifi.json`

The repository path is `Notifications/get_notifi.json`. Do not substitute a different path or URL.

## Important limitation — understand this before implementing

**GitHub alone cannot remotely push a notification to a phone at an exact time.** A raw GitHub URL only serves data; it does not wake every installed app or deliver push messages.

A GitHub-only approach is possible using **scheduled local notifications**:

1. The app fetches `get_notifi.json` when it opens and refreshes the feed.
2. It parses each notification and schedules a local Android notification on that user's device for the specified UAE date/time.
3. Android displays the scheduled notification at that time, even if the app is not currently open, subject to Android notification permissions, exact-alarm restrictions, device settings, force-stop behavior, and manufacturer battery restrictions.
4. Each device should fetch the entry and schedule it **before its scheduled time**. If a user is offline or the app has not fetched the entry yet, the app cannot know about it until a later fetch.
5. **Catch up after reconnect:** when the app next runs and internet is available, fetch the latest JSON. If a notification's scheduled time has passed but it has not been delivered, apply the overdue policy below and show it once when appropriate. This is delayed delivery, not delivery at the original scheduled time.

Do not claim that a GitHub commit by itself instantly pushes to all phones. No GitHub Actions workflow or server-side push script is required for this local-notification design. There is no fully reliable way to guarantee immediate fetch on every device using GitHub alone.

## Required JSON format

Use valid JSON. The `date` is the calendar date in Dubai/UAE, and `time` is the UAE local clock time. Use the IANA timezone `Asia/Dubai` in the Android scheduling code. Do not parse the time using the phone's local timezone; users may be travelling or have a different device timezone.

Example: a notification scheduled for **3:20 PM Dubai time on November 7, 2026**:

```json
{
  "notifications": [
    {
      "id": "2026-11-07-01",
      "type": "announcement",
      "title": "Store maintenance",
      "message": "Downloads may be slow tonight.",
      "package": "",
      "link": "https://t.me/my_dad_raaj",
      "date": "2026-11-07",
      "time": "3:20 PM"
    }
  ]
}
```

### JSON field rules

- `notifications`: required array of notification objects.
- `id`: required unique string. Use it to avoid scheduling or showing the same notification more than once.
- `type`: string; support the existing app's types where applicable, such as `announcement`, `update`, and `new_app`. Do not crash on an unknown type.
- `title`: required notification title.
- `message`: required notification body.
- `package`: optional string; it may be empty.
- `link`: optional string; open it when the user taps the notification if the existing app supports links. Validate the URL before opening it.
- `date`: required `YYYY-MM-DD` date in Dubai time.
- `time`: required 12-hour time string such as `3:20 PM` or `11:05 AM`, interpreted in `Asia/Dubai`.
- `versionCode`: optional number for update notifications if the existing app needs it. Keep support for it if already used.

Do not write a comma after the final property in a JSON object. Invalid JSON must be handled safely without crashing the app.

## Required Android app behavior

Inspect the existing project and follow its current language, architecture, UI, package name, and notification style. Make focused changes only; do not overwrite unrelated features.

1. **Fetch from GitHub:** Make an HTTP GET request to the exact raw URL above using the app's existing networking approach, or add a lightweight compatible approach if needed. Do not put secrets in the app. Handle no internet, HTTP errors, invalid JSON, and timeouts gracefully.
2. **Parse the feed:** Read the top-level `notifications` array and validate required fields.
3. **Interpret Dubai time correctly:** Combine `date` and `time`, parse with a strict 12-hour formatter, and apply `ZoneId.of("Asia/Dubai")` (or the equivalent in the project's language). Do not use the device's default timezone.
4. **Schedule local notifications:** Schedule a local Android notification for each valid future item. Use a stable notification/request identifier derived from `id`, so refreshes do not create duplicates. Use the app's existing notification channel if suitable; otherwise create a channel on Android 8.0+.
5. **Permissions:** Request Android 13+ `POST_NOTIFICATIONS` permission appropriately. For exact timing, investigate Android's exact-alarm requirements for the target SDK and request the applicable special access only when legally/technically appropriate. If exact alarms are unavailable, explain that Android may delay delivery rather than promising an exact time.
6. **Survive restart:** If the app's architecture and Android restrictions allow it, reschedule pending future notifications after device reboot and after relevant time/timezone changes. Follow current Android permission and platform rules.
7. **Avoid duplicates:** Persist scheduled/processed IDs using the project's existing persistence approach. Re-fetching the same JSON must not show a notification repeatedly. If an existing ID's date/time changes, cancel and reschedule that notification. If an entry is removed from the JSON, cancel its pending local schedule if practical, but do not delete unrelated app notification history.
8. **Refresh the feed and recover after reconnect:**
   - Fetch on app launch, whenever the app returns to the foreground, and on manual refresh if available.
   - Use WorkManager for best-effort background refresh with a connected-network constraint. Use a unique work name so refresh jobs do not multiply. Periodic WorkManager is inexact (minimum interval is generally 15 minutes); do not describe it as an instant or exact-time fetch.
   - When connectivity returns while the app/process is alive, enqueue a one-time refresh. Do not rely on a connectivity broadcast to wake a force-stopped app; Android may restrict background launches.
   - On every successful fetch, schedule all valid future notifications and check for overdue notifications that have not already been handled.
   - **Overdue policy:** for each unique `id`, if its date/time is in the past and it has not been delivered, show it once after a successful fetch only if it is within a configurable catch-up window (default: 24 hours). Skip older items by default to avoid a flood of stale alerts. Persist delivery state so a later refresh does not show it again. If product requirements prefer every overdue notification regardless of age, make that policy explicit and still deduplicate.
   - Keep network fetches off the main/UI thread. Add sensible timeouts and avoid rapid retry loops; let WorkManager apply backoff.
9. **Tap action:** When a notification is tapped, open the relevant existing app screen and/or its `link`, using safe URL handling and the app's existing navigation patterns.
10. **Past entries and offline recovery:** Do not schedule a past time as a future alarm. On successful fetch, apply the overdue policy in item 8: show eligible undelivered entries once, and skip expired/old entries. Never repeat an already delivered notification.
11. **Testing:** Test with a time a few minutes in the future; verify Dubai timezone while the device timezone differs; test permission denial, internet off before schedule time then restored after it, app never opened until after the schedule time, duplicate refresh, edited/removed JSON entries, app restart, device reboot, and force-stop behavior where supported. Clearly document Android restrictions and do not promise guaranteed delivery if the app has not fetched the item in advance.

## Remove Firebase instructions from the old setup

This project must use GitHub as the JSON host and Android local notifications only. Remove Firebase-specific setup and code from this guide, including:

- Firebase project creation and `google-services.json` instructions.
- `FIREBASE_SERVICE_ACCOUNT` GitHub secret.
- Firebase Cloud Messaging topic subscriptions.
- `.github/workflows/send-notifications.yml` and `.github/scripts/send_notifications.py` when their only purpose is sending FCM pushes.
- Claims that committing the JSON sends a remote push to all devices.

Do not delete Firebase code or configuration from the user's actual app automatically if other app features depend on it; first inspect references and remove only notification-specific Firebase dependencies when safe. The goal is to make this notification feature GitHub-database-only.

## How I publish a scheduled notification

1. Edit `Notifications/get_notifi.json` in my GitHub repository.
2. Add a new object with a unique `id`, `title`, `message`, `date`, and `time`.
3. Commit the JSON file.
4. Each user's app should fetch the updated JSON before the scheduled time and schedule the local notification on that device. If it fetches only after the time, the app can show the item once as a delayed catch-up notification if it is within the configured catch-up window. The commit alone does not send a push or wake the app.

## Instructions to the AI coding assistant

Read the existing Android project first. Then implement the GitHub JSON fetch + Dubai-time parsing + local notification scheduling in the existing app, using the project's actual language and architecture. Update Android manifest permissions, notification channels, scheduling, persistence/deduplication, tap behavior, and reboot handling as appropriate. Do not assume a package name or invent files before inspecting the project. Do not add Firebase or another push provider. Report which files were changed, what permissions the app needs, how the reconnect/catch-up behavior works, and the limitations that prevent a GitHub-only solution from guaranteeing delivery to every user at an exact time.
