# 001 — Restore the card-deck motion

- **Status**: TODO
- **Commit**: NO_GIT_COMMIT (the project has no Git commit metadata)
- **Severity**: HIGH
- **Category**: Physicality & interruptibility
- **Estimated scope**: 1 Android source file, roughly 120–180 lines changed

## Problem

The prototype makes the card deck feel physical: the front card follows the
finger, the two cards behind it lift and scale with gesture progress, the
front card leaves the viewport in 280ms, and the remaining cards settle into
their new slots. The Android implementation currently jumps between slot
values and only changes a raw translation value, so the deck snaps instead of
moving as one object.

Prototype evidence:

```css
/* prototype/styles.css:249-275 — current prototype */
.home-card {
  transform: translate3d(var(--drag-x, 0px), calc(var(--drag-y, 0px) - var(--gesture-lift, 0px)), 0)
    rotate(calc(var(--slot-rotate, 0deg) + var(--drag-rotate, 0deg)))
    scale(calc(var(--slot-scale, 1) + var(--gesture-scale, 0)));
  transition:
    transform 540ms var(--ease),
    opacity 320ms ease,
    box-shadow 320ms ease;
}

.home-card.is-flinging {
  transition:
    transform 280ms cubic-bezier(0.18, 0.85, 0.28, 1),
    opacity 280ms ease;
}
```

```js
// prototype/app.js:443-489 — current prototype gesture path
const progress = Math.min(Math.abs(gesture.dx) / threshold, 1);
const rotation = Math.max(-6, Math.min(6, (gesture.dx / width) * 5));
card.style.setProperty("--drag-x", `${gesture.dx}px`);
card.style.setProperty("--drag-y", `${Math.max(-14, Math.min(14, gesture.dy * 0.12))}px`);
card.style.setProperty("--drag-rotate", `${rotation}deg`);
renderDeck(progress);
...
card.classList.add("is-flinging");
card.style.setProperty("--drag-x", `${direction * width * 1.35}px`);
card.style.setProperty("--drag-y", `${Math.max(-22, Math.min(22, current.dy * 0.16))}px`);
card.style.setProperty("--drag-rotate", `${direction * 10}deg`);
card.style.opacity = "0";
window.setTimeout(() => {
  state.deck.push(state.deck.shift());
  card.classList.remove("is-flinging");
  renderDeck();
}, 300);
```

Android evidence:

```kotlin
// android/app/src/main/java/com/unfinished/app/MainActivity.kt:268-283
val animatedX = slot.x.dp
val animatedY = slot.y.dp
val animatedRotation = slot.rotation
val animatedScale = slot.scale
Card(
    modifier = modifier
        .offset(animatedX, animatedY)
        .fillMaxWidth()
        .height(430.dp)
        .graphicsLayer {
            rotationZ = animatedRotation
            scaleX = animatedScale
            scaleY = animatedScale
            translationX = dragX
            translationY = dragY
        }
```

```kotlin
// android/app/src/main/java/com/unfinished/app/MainActivity.kt:289-301
if (moved && kotlin.math.abs(dragX) >= 82f) {
    val direction = if (dragX >= 0f) 1 else -1
    dragX = direction * 1200f
    suppressClickUntil = System.currentTimeMillis() + 600
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    scope.launch {
        delay(280)
        dragX = 0f
        dragY = 0f
        moved = false
        onSwipe()
    }
}
```

The Android code has no `animate*`, `Animatable`, `spring`, or `tween` call
for the deck. `slot.x`, `slot.y`, `slot.rotation`, and `slot.scale` therefore
change synchronously when `order` rotates.

## Target

Implement the same motion sequence in `MainActivity.kt` while keeping the
existing card content, order, colors, and gesture threshold unchanged:

1. During the drag, write only `graphicsLayer` transform values. The front
   card follows the pointer directly; `translationY` is clamped to `-14..14`
   px and drag rotation is clamped to `-6..6` degrees.
2. Derive `progress = min(abs(dragX) / max(82f, width * 0.28f), 1f)` and apply
   the prototype's behind-card response: slot 1 lifts by `progress * 16f` px
   and scales by `progress * 0.03f`; slot 2 lifts by `progress * 8f` px and
   scales by `progress * 0.02f`.
3. When the gesture is released below the threshold, animate `dragX`, `dragY`,
   and `dragRotation` back to zero with
   `spring(dampingRatio = 0.8f, stiffness = 300f)` so an interrupted gesture
   carries velocity instead of snapping.
4. When the threshold is met, animate the front card to
   `direction * width * 1.35f`, `direction * 10f` degrees, and opacity `0f`
   with a 280ms `tween` using
   `CubicBezierEasing(0.18f, 0.85f, 0.28f, 1f)`. Trigger the existing haptic
   feedback once when the threshold is crossed, not only after release. Set
   `suppressClickUntil` to 520ms, matching the prototype's click guard.
5. After the fling animation completes, rotate `order` exactly once. Animate
   the three slot transforms into their new positions with a 300ms
   `tween` using `CubicBezierEasing(0.23f, 1f, 0.32f, 1f)`. The prototype uses
   540ms for this settle; the native target caps it at 300ms so the frequent
   deck interaction stays responsive while preserving the same path.
6. Animate `translationX`, `translationY`, `rotationZ`, `scaleX`, `scaleY`,
   and `alpha` only. Do not animate width, height, padding, margin, or layout
   offsets.

## Repo conventions to follow

- Card placement and z-order are defined by `CardSlot` and the `when (index)`
  block at `MainActivity.kt:226-231`; keep those values and the
  `order.drop(1) + order.first()` rotation behavior.
- The existing UI uses `graphicsLayer` for gesture transforms at
  `MainActivity.kt:277-283`; extend that pattern rather than introducing a
  second card-rendering component.
- Prototype motion tokens are `--ease: cubic-bezier(0.16, 1, 0.3, 1)` at
  `prototype/styles.css:18` and the fling curve at `:287`. Reproduce these
  curves exactly in the shared Android motion tokens from plan 003.

## Steps

1. In `MainActivity.kt`, add per-card `Animatable<Float>` values for drag X,
   drag Y, drag rotation, alpha, and the slot transform values. Keep the
   existing `remember(kind)` lifetime so a card cannot inherit another card's
   drag state.
2. Replace the raw `dragX = ...` fling assignment with a coroutine that
   animates all three transform values and alpha to the target using the exact
   280ms fling spec. Guard the coroutine with a `Job` so a new gesture cancels
   the previous animation cleanly.
3. During `onDrag`, update the animatable snap values directly and compute the
   behind-card lift/scale from the normalized progress. Keep the existing
   82dp minimum threshold and horizontal-only swipe semantics.
4. Move the `onSwipe()` call to the completion callback of the fling animation,
   then reset alpha and transforms after `order` has rotated so the recycled
   card re-enters at its back slot.
5. Replace static slot `.offset(animatedX, animatedY)` animation with
   transform-only animated slot values. Preserve the current `CardSlot`
   integer values and `zIndex` ordering.
6. Add a threshold-crossed boolean so the existing haptic feedback fires once
   per gesture at the same point that the prototype confirms the swipe.
7. Add a reduced-motion branch supplied by plan 003: remove translation and
   rotation interpolation, keep the alpha feedback, and snap the deck to the
   next slot after the same state transition.

## Boundaries

- Do NOT change card copy, colors, card height, slot values, z-order, swipe
  threshold, navigation, or persistence behavior.
- Do NOT add an animation library or a new gesture framework dependency.
- Do NOT animate layout properties; all motion must remain in
  `graphicsLayer`/`alpha`.
- Do NOT change the web prototype in this plan.
- If `CardSlot` or `MemoryCard` has moved from the cited lines, stop and refresh
  the plan rather than guessing.

## Verification

- **Mechanical**: run `android/gradlew.bat --offline --no-daemon :app:assembleDebug`.
  Expected result: `BUILD SUCCESSFUL` and a new
  `android/app/build/outputs/apk/debug/app-debug.apk`.
- **Feel check**: on a real device, drag the front card slowly, quickly, and
  below threshold. Confirm the front card follows the finger, the rear cards
  lift/scale, and a cancelled drag returns with a short spring instead of a
  snap.
- **Feel check**: swipe past the threshold and confirm the front card exits in
  280ms with a slight rotation, the haptic fires once at threshold, and the
  next cards settle as a deck with no frame where all three occupy the same
  slot.
- **Feel check**: interrupt a settle with a second swipe. Confirm the current
  transform is retargeted rather than restarting from the original slot.
- **Accessibility**: enable Android's system animator-duration scale 0 and
  confirm position/rotation movement is dropped while the card still fades and
  state changes remain understandable.
- **Done when**: the Android deck has the same gesture phases as the prototype,
  no visible slot teleport remains, and the debug APK passes the existing
  `aapt dump badging`/`apksigner verify` checks.
