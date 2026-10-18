# Prototype verification

Browser checks use an isolated, sandbox-enabled headless Microsoft Edge context. They do not attach to an existing browser profile or access device data.

Verified in this prototype:

- Ocean opening, ebb reveal, replay and skip.
- All three home compositions; quiet/encounter and day/night states.
- Clue request, direct reading, and deferral.
- Empty writing validation; in-memory draft retention and demonstration sealing.
- Search results and empty-result state.
- Manual and operating-system reduced-motion preferences.
- Embedded Chinese font loading.
- No document-wide horizontal overflow at viewport widths 320, 375, 768 and 1440 px.
- No page errors or failed HTTP resources in the browser smoke run.

Run the dev server first, then `node capture-preview.mjs` to repeat the checks and regenerate `screenshots/`. `npm run build` verifies the isolated frontend build. The build emits client-directive warnings from the animation library; there is no server rendering in this prototype.

These checks are not native Android verification, real-device performance testing, a product acceptance decision, or validation of the V3 algorithm. Terrain is an abstract exploration, not a physical tide simulation. Demo input disappears on refresh.

## Previous abstract home refinement — 2026-10-03 (superseded; not user accepted)

This change is limited to the abstract prototype. The realistic prototype is a visual reference, not an edit target. Its source was compared with the pre-work snapshot and matches.

The original smoke run passed after the morphology changes, including A/B/C, day/night, writing/draft/sealing, clues/reading/deferral, search, reduced motion and local fonts. Viewports 320, 375, 768 and 1440 px have no document-wide horizontal overflow; expanded C clue content reserves at least 12 px before navigation in these checks. Static terrain uses fewer than 20 SVG paths, combined into material layers, for 788–868 procedural channels depending on composition.

Actual rendered A/B/C home screens, all three curved retreat edges, night C and narrow C were visually reviewed. The final smoke replay passed after the curved-edge correction and checks the different retreat directions in all three compositions. The final `npm run build` also passed; client-only directive warnings remain non-blocking.

Updated captures are saved to `screenshots/tidal-tree-refinement/`; old capture files are preserved. Source before this change is backed up under `.history/2026-10-03-before-tree-refinement/`. No photographic textures were embedded and no new image-generation asset is used in the abstract UI. Native performance, larger accessibility text scales and user acceptance remain unverified.

## Drainage / incision proxy — 2026-10-03

Research preceded this replacement; see `TIDAL-FORMATION-RESEARCH.md`. A predefined cubic trunk and recursive alternating branches are no longer used. The current network comes from weighted routing on a deterministic irregular planar triangulation, contributing-area accumulation, two illustrative conductivity feedback passes and a dimensionless incision threshold. This is not a Poisson water-surface model or a hydrodynamic simulation.

The Node + isolated Edge smoke checks passed for all three compositions. They check: every land node reaches the sea outlet; routing potential strictly decreases; accumulated flux does not decrease downstream; adjoining reaches share exactly the same junction coordinates and width; smoothed reaches widen downstream; replay retains identical geometry; and the illustrative height field is finite. The model has 215–308 reaches and 104–148 confluences, not a required decorative branch count. Fewer than 20 static SVG paths combine the materials.

The opening now lowers a water level through the same derived incised field, not a translated horizontal curtain or branch-growth animation. Low channel water remains in the static shore composition. Background currents are masked to sea/channel water, rather than painted across the mudflat. Both manual and system reduced-motion modes skip the reveal.

Build and existing interaction checks passed: A/B/C, quiet/encounter, day/night, clues/direct-reading/deferral, writing validation/RAM draft/seal demonstration, search/empty states, embedded font, reduced motion. Widths 320/375/768/1440 have no document-wide horizontal overflow; narrow C expanded clues keep >=12 px from navigation. No browser errors or failed HTTP resources occurred.

Rendered A/B/C shore screens, height-field ebb frames, C night and narrow states were inspected. A first regular-grid attempt produced angular comb-like channels and was replaced with irregular planar routing before handoff. New captures are in `screenshots/drainage-model/`; all older captures and `.history/2026-10-03-before-drainage-model/` are preserved.

No source/artwork in the realistic prototype, Android App, V3 algorithm, backlog, Git remote or user memory records was changed in this iteration. Acceptance is deliberately pending: visual recognition must be assessed by the user. Native GPU performance, large text accessibility, physical tide accuracy and long-term sediment/vegetation evolution are not validated by this browser prototype.
