# Animation optimization plans

These plans address the gap between the Android app and the `prototype/`
motion reference. They are plans only; no Android source code has been
modified by this audit.

| Plan | Focus | Severity | Status | Depends on |
|---|---|---:|---|---|
| [003](003-motion-tokens-and-reduced-motion.md) | Shared curves, durations, press feedback, reduced motion | MEDIUM | TODO | — |
| [001](001-card-deck-motion-parity.md) | Gesture-following, rear-card lift, fling, slot settle | HIGH | TODO | 003 |
| [002](002-panel-transition-parity.md) | Picked-card lift and panel enter/exit | HIGH | TODO | 003 |

## Recommended execution order

1. Implement **003** first so the deck and panel plans share one motion
   vocabulary and accessibility branch.
2. Implement **001** next. It is the highest-frequency interaction and the
   largest source of the “prototype feels physical / app feels static” gap.
3. Implement **002** after the deck is stable, then verify the card-to-panel
   seam on a real device.

## Scope boundary

The current audit covers the Android home deck, card selection, function-panel
transition, action press feedback, and reduced motion. It does not change the
prototype, panel content state machines, database behavior, or backend.
