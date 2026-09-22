# 002 — Connect card selection to the function panel

- **Status**: TODO
- **Commit**: NO_GIT_COMMIT (the project has no Git commit metadata)
- **Severity**: HIGH
- **Category**: Physicality & cohesion
- **Estimated scope**: 1 Android source file, roughly 60–100 lines changed

## Problem

The prototype creates a spatial relationship between the selected card and its
function panel: the card lifts by 22px, straightens, and scales to 1.015; the
panel then rises from below the viewport with a 300ms ease-out and an 180ms
opacity fade. Android swaps the entire `HomeScreen` for `FunctionPanel` in one
composition pass, so the panel appears as an unrelated page and the selected
card never becomes the transition origin.

Prototype evidence:

```css
/* prototype/styles.css:406-450 — current prototype */
.home-card.is-picked {
  z-index: 20 !important;
  transform: translateY(-22px) rotate(0deg) scale(1.015) !important;
  box-shadow: 0 30px 72px rgba(42, 61, 70, 0.23);
}

.function-panel {
  opacity: 0;
  pointer-events: none;
  transform: translateY(102%);
  transition:
    transform 300ms var(--ease),
    opacity 180ms ease;
}

.function-panel.is-open {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}
```

```js
// prototype/app.js:492-503 — current prototype selection sequence
card.addEventListener("click", () => {
  if (Date.now() < state.suppressClickUntil) return;
  card.classList.add("is-picked");
  const delay = window.matchMedia("(prefers-reduced-motion: reduce)").matches ? 0 : 250;
  window.setTimeout(() => openPanel(card.dataset.card), delay);
});
```

Android evidence:

```kotlin
// android/app/src/main/java/com/unfinished/app/MainActivity.kt:123-145
var selectedCard by remember { mutableStateOf<CardKind?>(null) }
...
} else if (selectedCard == null) {
    HomeScreen(
        surface = surface,
        onCardClick = { selectedCard = it },
        onSettingsClick = { selectedCard = CardKind.Settings }
    )
} else {
    FunctionPanel(
        kind = selectedCard!!,
        repository = repository,
        aiProvider = aiProvider,
        surface = surface,
        onSurfaceChange = { surface = it },
        onBack = { selectedCard = null }
    )
}
```

## Target

Make the Android root own the transition state instead of conditionally
removing one screen and inserting another:

1. Add `openingCard: CardKind?` and `selectedCard: CardKind?` to
   `UnfinishedApp`. On a valid content-card click, set `openingCard`, run a
   250ms delay, then set `selectedCard`; use 0ms when reduced motion is
   enabled. The Settings button opens directly without the card-pick delay.
2. Pass `openingCard` to `HomeScreen` and animate only that card to
   `translationY = -22.dp`, `rotationZ = 0f`, `scale = 1.015f`, and the
   prototype's stronger shadow while the delay runs.
3. Wrap the home/panel branch in `AnimatedContent` or equivalent
   `AnimatedVisibility` so the panel enters with
   `slideInVertically(tween(300, CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))`
   from `102%` of its own height plus `fadeIn(tween(180))`.
4. Exit with the exact reverse values: `slideOutVertically` over 300ms with
   the same curve plus `fadeOut(tween(180))`. The home screen must remain
   underneath so the panel appears attached to the selected card.
5. Keep panel colors tied to `kind.color` in `FunctionPanel` and preserve the
   existing `onBack` semantics. Do not add a bottom navigation bar.
6. When reduced motion is enabled, remove the vertical slide and keep only the
   180ms opacity fade; keep the 250ms selection delay at 0ms.

## Repo conventions to follow

- The panel already derives its three-stop gradient from `kind.color` at
  `MainActivity.kt:364-384`; keep that color ownership.
- `PanelScaffold` is the existing panel content wrapper at
  `MainActivity.kt:388-403`; animate its containing surface, not individual
  form rows.
- The prototype uses `--ease: cubic-bezier(0.16, 1, 0.3, 1)` at
  `prototype/styles.css:18`, and the panel duration pair is 300ms/180ms at
  `:427-429`. Reuse the shared Android motion tokens from plan 003.

## Steps

1. Add `openingCard` and a `LaunchedEffect(openingCard)` coroutine in
   `UnfinishedApp`; cancel it when the card is swiped, the panel opens, or the
   user presses back.
2. Extend `HomeScreen` with an `openingCard` parameter and pass an
   `isPicked` flag to `MemoryCard` only for the selected card.
3. In `MemoryCard`, animate the picked transform with
   `animateFloatAsState`/`Animatable` using 250ms selection timing and the
   exact `-22dp`, `1.015f`, and `0deg` targets.
4. Replace the root `if (selectedCard == null)` branch with a single
   transition container whose target state is `selectedCard == null`.
5. Configure enter/exit transitions with the exact 300ms slide and 180ms
   opacity specs. Use an initial offset of `height * 1.02f` to match CSS
   `translateY(102%)`.
6. Route `onBack` through the same transition state and clear `openingCard`
   after the exit completes so the home deck is ready for the next action.
7. Add the reduced-motion branch from plan 003 and ensure no slide or scale
   is applied in that branch.

## Boundaries

- Do NOT rewrite any panel content, step state machines, Room calls, AI calls,
  or color tokens.
- Do NOT change the selected card's click behavior, except to add the
  transition delay and cancellation described above.
- Do NOT add Navigation Compose or a new dependency.
- Do NOT animate each form row; this plan concerns the card-to-panel seam.
- If the root state shape has changed from the cited lines, stop and refresh
  this plan rather than improvising.

## Verification

- **Mechanical**: run `android/gradlew.bat --offline --no-daemon :app:assembleDebug`.
  Expected result: `BUILD SUCCESSFUL`.
- **Feel check**: tap each of the three cards. Confirm the selected card lifts
  and straightens for 250ms before the matching color panel rises from below.
- **Feel check**: press back during panel entry and during panel exit. Confirm
  the transition retargets cleanly with no flash of the wrong card or color.
- **Feel check**: open Settings and verify it uses the neutral settings color
  while the same panel motion is retained.
- **Accessibility**: enable reduced motion and confirm the panel fades without
  vertical travel while content remains readable and focusable.
- **Done when**: no card-to-panel change is an instantaneous page swap, panel
  color still comes from `kind.color`, and back navigation has no visible
  teleport or double-render.
