# 003 — Establish shared motion tokens and accessibility behavior

- **Status**: TODO
- **Commit**: NO_GIT_COMMIT (the project has no Git commit metadata)
- **Severity**: MEDIUM
- **Category**: Accessibility & cohesion
- **Estimated scope**: 2 Android source files, roughly 50–90 lines changed

## Problem

The web prototype has an explicit motion vocabulary and a reduced-motion
branch. Android has no shared duration/easing tokens, no `prefers-reduced-
motion` equivalent, and no press feedback beyond Material's default behavior.
This makes every native animation a new one-off and leaves users who disable
system animations with the same heavy movement.

Prototype evidence:

```css
/* prototype/styles.css:1-19 — current prototype tokens */
:root {
  --ease: cubic-bezier(0.16, 1, 0.3, 1);
}
```

```css
/* prototype/styles.css:1178-1186 — current prototype reduced-motion branch */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    transition-duration: 1ms !important;
    animation-duration: 1ms !important;
    animation-iteration-count: 1 !important;
  }
}
```

Android evidence:

```kotlin
// android/app/src/main/java/com/unfinished/app/MainActivity.kt:460-485
private fun PrimaryAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
        modifier = Modifier.heightIn(min = 48.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}
...
private fun TextAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, colors = ButtonDefaults.textButtonColors(contentColor = Accent), modifier = Modifier.heightIn(min = 48.dp)) { Text(text) }
}
```

The Android source contains no motion token object and no explicit
`prefers-reduced-motion`/animator-scale check.

## Target

Create one small motion module and use it from plans 001 and 002:

```kotlin
// android/app/src/main/java/com/unfinished/app/MotionTokens.kt — target
package com.unfinished.app

import androidx.compose.animation.core.CubicBezierEasing

internal object MotionTokens {
    val easeOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val prototypeEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val flingEase = CubicBezierEasing(0.18f, 0.85f, 0.28f, 1f)
    const val pressMillis = 140
    const val opacityMillis = 180
    const val panelMillis = 300
    const val flingMillis = 280
}
```

The executor may place the object in `MainActivity.kt` if extracting a file
would create unnecessary churn, but the names and values must remain shared.

Detect reduced motion from the Android global animator duration scale through
the existing `LocalContext`/`ContentResolver`; treat a value of `0f` as
reduced motion. Do not request a new permission. In reduced motion:

- Card and panel translations, rotation, and scale snap to their final state.
- Opacity/color feedback remains at 180ms where it aids comprehension.
- Press scale is omitted; keep only the platform's non-spatial color/ripple
  feedback.

Add a 0.97 press scale to `PrimaryAction`, `SecondaryAction`, `TextAction`,
the settings button, and the front card using a 140ms `easeOut` spec. Do not
add hover-only effects to this touch-first app.

## Repo conventions to follow

- Keep design constants near the existing top-level color constants at
  `MainActivity.kt:80-92`, or extract them to the adjacent package with no
  dependency changes.
- Reuse `graphicsLayer` for press transforms, as already used by
  `MemoryCard` at `MainActivity.kt:277-283`.
- Keep the CSS prototype tokens unchanged; the Android tokens are an adapter
  that mirrors them, not a second visual design system.

## Steps

1. Add `MotionTokens.kt` (or the equivalent top-level object in
   `MainActivity.kt`) with the exact curves and durations above.
2. Add a small `rememberReducedMotion()` composable that reads
   `Settings.Global.ANIMATOR_DURATION_SCALE`, observes configuration changes,
   and returns `true` only when the scale is exactly `0f`.
3. Thread the reduced-motion flag into the card-deck and panel transition
   implementations from plans 001 and 002.
4. Add a reusable `PressScale` modifier using
   `MutableInteractionSource`/`collectIsPressedAsState` and
   `animateFloatAsState`. Target `0.97f` while pressed and `1f` at rest with
   140ms `MotionTokens.easeOut`; bypass the scale transform when reduced motion
   is active.
5. Apply `PressScale` to the named actions only. Do not apply it to text
   fields, scroll containers, or every list row.
6. Add a short developer note in `docs/` describing the token names and the
   reduced-motion behavior so future screens reuse the same values.

## Boundaries

- Do NOT change the web prototype's CSS or JavaScript timings.
- Do NOT add a motion/animation dependency.
- Do NOT turn reduced motion into a complete removal of opacity/color feedback.
- Do NOT add hover interactions; the product is touch-first.
- Do NOT modify data flow, navigation, persistence, or card geometry.

## Verification

- **Mechanical**: run `android/gradlew.bat --offline --no-daemon :app:assembleDebug`.
  Expected result: `BUILD SUCCESSFUL`.
- **Mechanical**: run `rg -n "CubicBezierEasing|panelMillis|flingMillis|pressMillis|ANIMATOR_DURATION_SCALE" android/app/src/main/java` and confirm every
  motion value comes from `MotionTokens` or the single reduced-motion helper.
- **Feel check**: press each primary/secondary/text action and the settings
  button. Confirm the 0.97 scale is subtle and returns faster than it enters.
- **Accessibility**: set Android animator duration scale to 0, reopen the app,
  and confirm cards/panels snap spatially but retain opacity/color feedback.
- **Done when**: plans 001 and 002 can consume the shared tokens without
  introducing inline durations or duplicate curves, and reduced motion is
  honored on a real device.
