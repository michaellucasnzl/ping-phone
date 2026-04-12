# PingPhone

An Android app that rings your phone on demand, even when it's on silent or Do Not Disturb. Send a trigger SMS from any phone and PingPhone will override the volume settings and blast an alarm until you find it and dismiss it.

---

## How It Works

1. PingPhone runs silently in the background, monitoring for incoming SMS messages.
2. When an SMS containing the trigger code (default: **777**) arrives, the app:
   - Disables Do Not Disturb / silent / vibrate mode
   - Sets the alarm volume to your chosen level (default: 70%)
   - Plays a loud looping alarm tone
   - Shows a full-screen dismiss button, even over the lock screen
3. Tap **Dismiss** to stop the alarm. PingPhone automatically restores your previous volume and silent-mode settings.

---

## Features

- **SMS trigger** — any phone can trigger the alarm by texting the code; no internet required
- **Overrides silent/DND mode** — works even when the phone is muted or on Do Not Disturb
- **Configurable trigger code** — change the default `777` to anything you like
- **Adjustable volume** — set the alarm volume as a percentage of max (10–100%)
- **Custom alarm sound** — pick any system alarm tone
- **Test button** — simulate a trigger from within the app to verify everything works
- **Persistent monitoring notification** — always-visible status indicator confirming the app is active
- **Boot persistence** — automatically resumes monitoring after a reboot

---

## Requirements

- Android 8.0 (API 26) or higher
- Permissions required:
  - **Receive SMS / Read SMS** — to detect the trigger message
  - **Do Not Disturb access** — to override silent/DND mode (granted via a special system settings screen)
  - **Modify audio settings** — to change the ringer volume
  - **Post notifications** — to show the monitoring status and alarm notification (Android 13+)

---

## Building from Source

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable) with the Android SDK installed
- A connected Android device or emulator (API 26+)

### Steps

1. Clone the repository:
   ```
   git clone https://github.com/your-username/ping-phone.git
   cd ping-phone
   ```

2. Open the project in Android Studio:
   - **File → Open** → select the `ping-phone` folder
   - Wait for Gradle sync to complete

3. Build the debug APK:
   - **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
   - Or from the terminal:
     ```
     .\gradlew assembleDebug
     ```
   - The APK will be at `app\build\outputs\apk\debug\app-debug.apk`

---

## Installing on Your Phone (Sideloading)

PingPhone is not on the Play Store. Install the APK directly:

### Option A — via USB (ADB)

1. Enable **Developer Options** on your phone:
   - Settings → About Phone → tap **Build Number** 7 times
2. Enable **USB Debugging** in Developer Options
3. Connect your phone via USB and run:
   ```
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

### Option B — copy the APK to your phone

1. Copy `app-debug.apk` to your phone (USB, Google Drive, email, etc.)
2. On the phone, enable **Install from Unknown Sources**:
   - Settings → Apps → Special App Access → Install Unknown Apps → select your file manager → Allow
3. Open the APK file with your file manager and tap **Install**

---

## First Launch

On first launch, PingPhone will walk you through three permission steps:

1. **SMS access** — tap Grant and allow both SMS permissions in the system dialog
2. **Do Not Disturb access** — tap Grant; the system settings page opens — find PingPhone in the list and toggle it on, then return to the app
3. **Notifications** (Android 13+ only) — tap Grant and allow in the system dialog

Once all permissions are green, the main screen is shown with a **"Monitoring for trigger SMS"** status indicator.

---

## Usage

1. Leave PingPhone installed on the phone you want to be able to find.
2. When the phone is lost (e.g. down the back of the sofa), send an SMS containing `777` to it from any other phone.
3. The lost phone will immediately start alarming at full volume regardless of its current sound setting.
4. Follow the sound, pick it up, and tap **Dismiss**.

### Customising the trigger code

Open PingPhone → edit the **Trigger Code** field → tap **Save**. The new code takes effect immediately; no restart needed.

---

## Project Structure

```
app/src/main/
├── java/com/michaellucas/pingphone/
│   ├── PingPhoneApplication.kt       # Notification channel setup
│   ├── data/
│   │   └── SettingsRepository.kt     # DataStore-backed persistent settings
│   ├── viewmodel/
│   │   └── SettingsViewModel.kt      # UI state + permission state
│   ├── receiver/
│   │   ├── SmsReceiver.kt            # Manifest-registered SMS broadcast receiver
│   │   └── BootReceiver.kt           # Restores monitoring notification after reboot
│   ├── service/
│   │   ├── AlarmService.kt           # Foreground service: DND override, volume, MediaPlayer
│   │   └── MonitoringNotificationHelper.kt
│   └── ui/
│       ├── MainActivity.kt
│       ├── AlarmDismissActivity.kt   # Full-screen dismiss screen (shows over lock screen)
│       ├── screen/
│       │   ├── MainScreen.kt         # Settings screen (trigger code, volume, sound picker)
│       │   └── PermissionOnboardingScreen.kt
│       └── theme/
└── res/
    ├── drawable/                     # Adaptive icon + notification icon
    └── values/                       # Strings, colours, theme
```

---

## License

MIT — see [LICENSE](LICENSE).
