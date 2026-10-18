# Startup image first-frame experiment — 2026-10-09

## Result

**The translucent-Activity experiment does not pass seamless-start acceptance.** On both Meizu 18s (API 33) and Meizu 21 (API 36), it avoids a solid-color frame after the launcher-to-Activity handoff, but the launcher remains visible while the process starts. The sea image therefore is not the first visible frame of the complete launch sequence. Do not port this approach to production as a fix. The earlier AAPT2 build blocker is no longer reproducible.

## Baseline evidence

- Device: Meizu 21, API 36, 1080×2340.
- Method: force-stopped only `com.unfinished.app`, returned to Home, screen-recorded, then launched `.MainActivity` via ADB. This is an ADB Activity launch, **not** a launcher-icon launch.
- Observed: at roughly 0.1 s, a rounded blue-grey starting window; around 0.25 s and 0.5 s, a solid blue-grey frame; around 0.8 s, the sea image and opening copy appeared.
- Recording: `<local-artifacts>\unfinished-startup-baseline-20261009b.mp4`.

## Isolated implementation prepared

- Added `startupImageExperiment` build type with package suffix `.startupexperiment` and label `未完成·首帧实验`; it cannot overwrite `com.unfinished.app`.
- Only this build type skips the app's SplashScreen keep-on-screen gate, synchronously seeds the Activity window with the sea image selected from local time and the saved night setting, keeps Compose transparent while that same bitmap loads, and requests the transition to an opaque window after the first sea-content frame commits. If conversion fails, the seeded image remains as the window background instead of reverting to a solid color.
- The normal Debug/Release behavior is still gated by `STARTUP_IMAGE_EXPERIMENT=false`.

### Callback correction during device testing

The first installed test build crashed on Meizu 21 with `IllegalStateException: Cannot call removeOnDrawListener inside of onDraw` at `MainActivity.kt:302`. The experiment callback was corrected to be one-shot and to post listener removal after `onDraw`; the APK was rebuilt and installed over the isolated package on both phones. Subsequent cold launches completed without this crash, and the frame-commit logs reported successful opaque conversion.

## Initial build failure (not reproduced in follow-up)

Commands attempted:

```powershell
cd <project-root>\android
.\gradlew.bat :app:assembleStartupImageExperiment --stacktrace
.\gradlew.bat :app:processDebugResources --stacktrace --info
```

The first attempts reported a link failure at `process*Resources`:

```text
AAPT2 aapt2-8.13.0-13719691-windows Daemon #0: Unexpected error during link
Caused by: java.io.IOException: AAPT2 process unexpectedly exit. Error output:
```

The failure also occurred on ordinary Debug, so it was not evidence that the experiment theme itself was invalid. A direct AAPT2 link diagnostic returned Windows access-violation exit code `-1073741819` for manually assembled inputs; that manual invocation is diagnostic only and is not a substitute for Gradle's full resource-link command.

## Safety and device status

- The isolated package `com.unfinished.app.startupexperiment` (version 0.4.0, target SDK 36) was installed with `adb install -r` on Meizu 21 and Meizu 18s. It remains installed for later overwrite testing.
- The production package `com.unfinished.app` remains installed on both phones and was not overwritten. No app data was cleared and no package was uninstalled.
- Temporary screen recordings were removed from the phones after being pulled to the workstation. Local evidence is preserved at the paths listed below.
- The experiment variant remains isolated and is still gated off for normal Debug/Release builds.

## Device acceptance test — completed, acceptance failed

The current APK was verified as application ID `com.unfinished.app.startupexperiment`, distinct from production. Tests were cold launches after force-stop and return to Home, started with the explicit Activity component. An attempted MAIN/LAUNCHER ADB intent did not resolve, so these runs are **not** launcher-icon taps.

| Device | Launch result | Recording | Visual sample |
|---|---|---|---|
| Meizu 21, API 36, 1080×2340 | `am start -W` TotalTime 470 ms; recording 8.133800 s at ~26.9 fps | `<local-artifacts>\unfinished-startup-experiment-m21-retry1.mp4` | Launcher visible at sampled 0.00, 0.25 and 0.50 s; sea image visible around 0.75 s; opening copy appears later. |
| Meizu 18s, API 33, 1440×3200 | `am start -W` TotalTime 750 ms; recording 2.338678 s at ~59.4 fps | `<local-artifacts>\unfinished-startup-experiment-m18s-20261009.mp4` | Same ordering: launcher first, then sea image, then opening copy. |

The contact sheets sampled at 4 fps are `<local-artifacts>\unfinished-startup-experiment-m21-contact.png` and `<local-artifacts>\unfinished-startup-experiment-m18s-contact.png`. Device logs on both phones report `first startup content frame committed; opaque conversion succeeded=true`. No solid-color interval was observed after the image-backed Activity transition in these recordings, but the visible launcher gap is itself a failure of the intended seamless startup. Acceptance therefore **fails**: this approach changes the gap from a solid color to the Home screen rather than presenting the sea image as the first visible frame.

Current APK SHA-256: `3F408FA9ABE55D427942D57A44C6FA63EE1B6C3C29B4DB9CAABAE2DDAE5DE1D6`.

The test is complete and the translucent-Activity approach is rejected as a production fix. A separate experiment is needed to evaluate other startup-window strategies; keep the production startup configuration unchanged until a candidate passes on both devices.

## Follow-up investigation — 2026-10-09

The user correctly noted that the previous practice did not solve the solid-color interval. The isolated implementation was updated to seed the Activity window with an image before the first draw, then exercised on both phones. It removed the post-handoff solid-color frame in the captured path, but revealed the launcher beneath the translucent Activity until app content was ready; this is not an acceptable seamless-start result.

### New runtime attribution

A second cold launch on Meizu 21 (API 36), started with ADB after force-stop and returning Home, produced these `dumpsys window` observations:

- At approximately 160 ms, `mObscuringWindow` was `Splash Screen com.unfinished.app`.
- At approximately 400 ms, the Splash window was `EXITING` and `mObscuringWindow` was `com.unfinished.app/.MainActivity`.

This confirms the flat-color interval has two owners: first the Android starting window, then the app Activity. It is not accurate to attribute the whole interval solely to the operating system.

### Source diagnosis

- `android/app/src/main/res/values-v31/styles.xml` assigns the same solid `opening_startup_background` to `windowSplashScreenBackground` and `android:windowBackground`.
- `android/app/src/main/res/values/styles.xml` uses that solid color as the base Activity window background. The normal `MainActivity.onCreate` also sets it explicitly.
- Compose uses the same blue-grey fallback while waiting. The sea bitmap is decoded asynchronously on `Dispatchers.IO` by `rememberBitmapResource`.
- The normal Splash keep condition is released by a Compose `SideEffect` when the bitmap is available, not when a frame containing the bitmap is known to have committed. Bitmap availability alone is not a frame-presentation guarantee; a Splash-to-Activity handoff can therefore reveal the Activity's solid `windowBackground` before the image reaches the display.

### What was ruled out / remains unknown

- Earlier invocations reported AAPT2 link failures for both `:app:processDebugResources` and `:macrobenchmark:processDebugResources`. Follow-up now succeeds for a minimal Android module and for a forced full rerun of the app resource pipeline, so this is historical evidence only; the failure is not currently reproducible and its original trigger is unknown.
- A temporary comparison using AGP 8.13.2/AAPT2 8.13.2 failed at the same link stage. The project was restored to AGP 8.13.0 afterward.
- Earlier attempts with one Gradle worker, SDK Build Tools 35.0.0 AAPT2 override, writable `.android` cache, and resource no-compress settings also failed. The no-compress probe is not present in `android/app/build.gradle` now.
- A temporary `:macrobenchmark` comparison with `compileSdk/targetSdk 35` failed identically; a second comparison with that module's dependencies temporarily removed also failed identically. Both temporary edits were restored. These results make an app-only resource, Android 36-only platform, or benchmark dependency the less likely cause, but they do not identify the native crash source.
- `aapt2 dump resources` succeeds against the installed Android 36 `android.jar`, but this does not establish why the full link operation crashes. Exact AAPT2 root cause remains unknown; do not treat the hand-built partial link command as equivalent to Gradle's invocation.
- Follow-up isolation changed the result: a temporary Android-only module containing just a Manifest and one string resource completed `:aapt2probe:processDebugResources` successfully. The module was then removed and is not part of the project.
- Using the existing Gradle 8.14.3 distribution and dependency cache, `:app:processDebugResources --no-daemon --max-workers=1 --offline --rerun-tasks` also completed successfully, forcing all 15 resource-pipeline tasks to execute. Thus a full Gradle resource link is currently working; the earlier AAPT2 failure is not reproducible in this invocation. Its cause remains undetermined and should not be treated as a current blocker.
- A separate attempt using a fresh/custom Gradle cache failed before resource linking because app dependencies were absent from that cache; that result is unrelated to AAPT2.

### Current experiment and required validation

The experiment remains isolated from the production application ID. It synchronously sets the Activity window background to the full-bleed sea drawable selected from local time and the saved night setting, keeps Compose transparent while the same bitmap is decoded, then requests conversion to an opaque window after an image-bearing frame commits. On these Meizu devices, the translucent Activity exposes the launcher during startup. The transition avoids a solid-color handoff but does not meet the goal that the first visible frame be the sea image. AOSP's starting-window behavior can vary for translucent Activities, so the observed result should not be generalized to all OEMs; it is sufficient to reject this approach for the tested devices.

The corrected isolated APK builds successfully and has been installed/launched on both devices. APK path: `android/app/build/outputs/apk/startupImageExperiment/app-startupImageExperiment.apk`; package ID verified as `com.unfinished.app.startupexperiment`; current SHA-256 is `3F408FA9ABE55D427942D57A44C6FA63EE1B6C3C29B4DB9CAABAE2DDAE5DE1D6`. The existing production package and data remain unchanged.
