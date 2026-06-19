# FocusGuard

An Android 10+ app that silences calls and notification banners from specific contacts — without requiring you to be the default phone or SMS app.

## Stack

| Layer | Tech |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| DI | Hilt |
| Database | Room (local, schema exported to `app/schemas/`) |
| Background | WorkManager + ForegroundService |
| Call filtering | `CallScreeningService` (Android 10+) |
| Notification filtering | `NotificationListenerService` |

## Build & Run

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 35 installed
- Physical Android 10+ device (emulators don't support `CallScreeningService` for real calls)

### Steps
1. Clone the repo
2. Open in Android Studio and let Gradle sync
3. Run on a physical device
4. Grant permissions in the onboarding screen

> **Note:** The Gradle wrapper JAR (`gradle-wrapper.jar`) is not included in the repo.
> Android Studio will download it automatically on first sync. If you need it manually, run:
> `gradle wrapper --gradle-version 8.9` from the project root.

## Required Permissions

| Permission | Purpose | How to grant |
|---|---|---|
| `READ_CONTACTS` | Match callers to your contact list | Runtime prompt |
| Notification Listener | Suppress SMS/notification banners | Settings → Apps → Special app access → Notification access |
| Do Not Disturb | Optional DND integration | Settings → Sound → Do Not Disturb → Apps |
| Battery Optimization | Keep service alive on aggressive OEMs | Settings → Battery → Battery optimization → FocusGuard → Don't optimize |
| `POST_NOTIFICATIONS` | Show the persistent "Active" notification | Runtime prompt (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | Precise schedule activation | Settings → Apps → FocusGuard → Alarms & reminders |
| `SEND_SMS` | Auto-reply to blocked callers | Runtime (requested when auto-reply is enabled) |

## How Call Screening Works

Android 10+ lets any app register a `CallScreeningService` without being the default dialer. When a call arrives:

1. System binds `FocusGuardCallScreeningService.onScreenCall()`
2. We look up the caller in the system Contacts Provider
3. Evaluate against enabled rules in priority order, checking schedule slots
4. If a BLOCK rule matches → `setRejectCall(true)` + `setSkipNotification(true)`
   - `setDisallowCall(false)` = send to voicemail (caller hears greeting)
   - `setDisallowCall(true)` = silent drop (caller gets no response)
5. Emergency bypass: if the same number has called N times in Y minutes, we let them through

### Rule priority
Rules are evaluated highest-priority-first. The first matching rule wins. ALLOW rules short-circuit the rest.

## How Notification Suppression Works

`FocusGuardNotificationListenerService` receives all posted notifications from messaging/phone packages (`com.android.messaging`, `com.google.android.apps.messaging`, `com.samsung.android.messaging`, etc.). On match:

- `cancelNotification(sbn.key)` removes the banner from the status bar

**Limitation:** There is no pre-screening API for notifications. The banner appears for ~100–500ms before FocusGuard cancels it. Eliminating the flash requires being the default SMS app — not something FocusGuard requires.

## OEM-Specific Issues

### Samsung (One UI)
- **Device Care → Battery → Background usage limits → Never sleeping apps → Add FocusGuard**
- Disable: Settings → Apps → FocusGuard → Battery → Restrict background activity

### Xiaomi (MIUI / HyperOS)
- Security app → Permissions → Autostart → Enable FocusGuard
- Settings → Apps → Manage apps → FocusGuard → Battery saver → No restrictions

### OnePlus (OxygenOS)
- Settings → Battery → Battery optimization → All apps → FocusGuard → Don't optimize

### Huawei (EMUI / HarmonyOS)
- Settings → Apps → FocusGuard → Battery → Allow auto-launch + Allow background activity

### General tip
After granting all permissions, lock the FocusGuard app in the recent apps panel (long-press → lock icon). This prevents most OEM battery killers from terminating the process.

## Room Schema Migrations

Schema files are exported to `app/schemas/com.focusguard.data.db.FocusGuardDatabase/`.

When you add columns or tables, add an explicit migration rather than relying on destructive rebuild:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE rules ADD COLUMN testMode INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

Register it in `DatabaseModule.kt`:

```kotlin
Room.databaseBuilder(context, FocusGuardDatabase::class.java, "focusguard.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

## Testing

### Call screening (physical device required)
1. Install on a physical Android 10+ device
2. Create a BLOCK rule for a test contact
3. Call from that contact's number using a second phone
4. Expected: call goes to voicemail silently; no missed-call notification

### Notification suppression
1. Enable notification listener access for FocusGuard
2. Create a BLOCK rule for a contact (suppress notifications enabled)
3. Send an SMS from that number
4. Expected: notification banner briefly appears then disappears

### Emergency bypass
1. Create a BLOCK rule with emergency bypass (e.g., 3 calls in 5 minutes)
2. Call 3 times from the blocked number within 5 minutes
3. Expected: first 2 calls → voicemail; 3rd call → rings through

## Project Structure

```
app/src/main/java/com/focusguard/
├── FocusGuardApp.kt           Hilt application + WorkManager config
├── data/
│   ├── db/
│   │   ├── FocusGuardDatabase.kt
│   │   ├── dao/               RuleDao, ScheduleSlotDao, ContactTagDao, EmergencyBypassDao
│   │   └── entity/            RuleEntity, ScheduleSlotEntity, ContactTagEntity, EmergencyBypassEvent
│   └── repository/
│       └── RuleRepository.kt  Entity ↔ domain model mapping + CRUD
├── domain/
│   ├── model/                 Rule, Contact, FilterResult, ScheduleSlot, …
│   └── usecase/               EvaluateCallUseCase, EvaluateNotificationUseCase
├── service/
│   ├── FocusGuardCallScreeningService.kt
│   ├── FocusGuardNotificationListenerService.kt
│   ├── FocusGuardForegroundService.kt
│   └── ScheduleCheckWorker.kt
├── receiver/
│   ├── BootReceiver.kt
│   └── ScheduleAlarmReceiver.kt
├── ui/
│   ├── MainActivity.kt
│   ├── navigation/            Screen.kt, NavGraph.kt
│   ├── theme/                 Color, Type, Theme
│   ├── dashboard/             DashboardScreen + ViewModel
│   ├── contacts/              ContactPickerScreen + ViewModel
│   ├── rules/                 RuleWizardScreen + ViewModel (4-step wizard)
│   ├── settings/              SettingsScreen + ViewModel
│   └── onboarding/            OnboardingScreen + ViewModel
├── di/
│   ├── AppModule.kt           ContentResolver, SharedPreferences
│   └── DatabaseModule.kt      Room database + DAOs
└── util/
    ├── ContactQueryHelper.kt  ContentResolver queries
    ├── PermissionHelper.kt    Permission status checks
    ├── ScheduleEvaluator.kt   Time/day-of-week logic
    ├── PhoneNumberMatcher.kt  Area code + glob pattern matching
    └── AutoReplyManager.kt    SmsManager wrapper
```

## Known Limitations

- Brief notification flash on SMS suppression — platform constraint, not fixable without being default SMS app
- `CallScreeningService` may not be called on deeply sleeping devices — mitigated by battery optimization exemption and sticky foreground service
- Samsung Bixby Routines and Galaxy AI features may interfere with call handling — disable conflicting routines if unexpected behaviour occurs
- MIUI's built-in call blocker takes priority over `CallScreeningService` — check MIUI call settings if screening isn't working on Xiaomi devices
