# Macro Recorder Advanced

Android Java project. Features: accessibility overlay REC/STOP/PLAY, automatic persistence of the latest macro, configurable repeat count (0 = continuous), and playback speed (0.5x–2.0x).

## Build
Open this folder in Android Studio and build `app` > `assembleDebug`, or push it to GitHub and run the included Actions workflow. Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions and limitations
The app requires the user to explicitly enable its Accessibility Service. It records touch paths and replays gestures using Android Accessibility APIs. Android/system-protected screens and some apps may block gesture injection. The recorder uses a full-screen accessibility overlay while recording, so behavior can vary by device/Android version; test on a non-critical screen first. The latest macro is stored locally in app preferences; this build does not provide multiple named profiles or cloud sync.
