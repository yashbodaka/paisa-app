# Local Android Setup

This project is meant to be easy to run and edit on a laptop.

## Recommended Setup

1. Install Android Studio.
2. Open this folder: `d:\internship\paisa-app`.
3. Let Android Studio install:
   - Android SDK Platform 35
   - Android SDK Build-Tools 35
   - Android Emulator, if you want to test without a phone
4. Press Run for the `app` configuration.

Android Studio will create a local `local.properties` file with your Android SDK path. That file should stay private to your machine and is ignored by Git.

## Without Android Studio

You can also install the Android command-line tools locally inside this project:

```powershell
.\scripts\setup-android-sdk.ps1
```

The script downloads Google's Android command-line tools, asks you to accept the Android SDK licenses, installs Platform 35 and Build-Tools 35, and writes `local.properties`.

This keeps the setup local to the project in `.android-sdk`, which is ignored by Git.

## Terminal Commands

Run unit tests:

```powershell
.\scripts\test.ps1
```

Create a debug APK:

```powershell
.\scripts\build-debug-apk.ps1
```

The APK will be created at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Install the debug APK on a connected Android phone:

```powershell
.\scripts\install-debug-apk.ps1
```

## Common Fixes

If Gradle says `JAVA_HOME is set to an invalid directory`, update `JAVA_HOME` to a real JDK path.

For this machine, Java was installed at:

```text
C:\Program Files\Java\jdk-22
```

Android projects are happiest with JDK 17 or newer. Android Studio also includes a bundled JDK, which is usually the easiest option.

If Gradle says the Android SDK is missing, open the project in Android Studio once and let it create `local.properties`.

Or run:

```powershell
.\scripts\setup-android-sdk.ps1
```
