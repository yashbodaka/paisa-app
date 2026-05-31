# Paisa

Paisa is a native Android personal finance app focused on logging money activity in under 3 seconds.

Type or speak entries such as:

- `200 food`
- `5000 salary`
- `300 to Rahul`
- `300 from Aman`
- `+2000 freelance`

The app works offline and stores data locally on the device.

## Tech Stack

- Kotlin
- Jetpack Compose
- Room
- WorkManager
- Android speech recognition

## Run

1. Install Android Studio.
2. Open this folder.
3. Let Android Studio install/sync the required Android SDK and Gradle dependencies.
4. Run the `app` configuration on an emulator or Android device.

You can also run from a terminal after Android Studio has installed the Android SDK:

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK will be created at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Test

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

The most important tests cover the quick-entry parser and summary calculations.

Convenience scripts are available:

```powershell
.\scripts\setup-android-sdk.ps1
.\scripts\test.ps1
.\scripts\build-debug-apk.ps1
.\scripts\install-debug-apk.ps1
```

See `docs/local-android-setup.md` for laptop setup notes and common fixes.
