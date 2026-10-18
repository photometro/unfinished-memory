# 留白母图 v2 — edit record

- Built-in image generation, 2026-10-04; edit target is the original generated noon shore, not candidate v1.
- Input 1: `public/environment/four-period/shore-noon.png`, edit target.
- Input 2: user's second tidal-tree photo, structure-only reference; no copied photo or watermark.
- Output: `public/environment/candidates/shore-master-v2.png`; original output retained under the Codex generated-images directory, exact location in ASSETS.md.
- Only one neutral-daylight candidate, no four-period derivatives. Homepage/control layout and functional palettes are unchanged.

## Exact prompt

```text
Use case: precise-object-edit.
Asset type: ONE portrait natural environmental texture for an existing mobile app homepage, not a UI mockup.
Input image 1 is the EDIT TARGET: the original generated noon tidal-flat texture. Input image 2 is ONLY a structural reference for fine dendritic tidal-creek branching; do not copy its composition, colours, watermark, or photographic pixels.
Primary request: preserve image 1's successful UI-friendly composition and muted warm-sand / mist-blue palette; improve only the tidal creek morphology in its middle-lower and lower half.
Invariants: preserve the straight-down aerial camera, portrait 1:2 framing, quiet warm ivory-grey upper half, broad winding trunk path and estuary placement, and the generous quiet grey-blue water in the bottom-right. The upper roughly 50–52% must remain low-detail pale damp silt suitable for dark headline and subtitle, with only delicate sediment grain, no new dark channels behind text. Keep the original soft diffuse neutral daylight, restrained shadows and gentle colour contrast, not the cooler dense all-over illustration of a different scene. Preserve open water at the bottom for translucent navigation. Keep the broad composition and main creek scale stable.
Change only the lower land morphology: replace the chunky raised sand-island / canyon-like banks with nearly flat low-relief damp mudflat, shallow recessed channels with low soft edges. Around the EXISTING main trunk and tributaries, develop more coherent tapering secondary creeks and dense, slender irregular feather-like tertiary drainage channels branching landward, inspired by image 2. Concentrate those recognizable dendritic details below the text area, do not distribute them evenly across the whole frame or grow them into the upper half. One broad main creek leads into the same open sea; branches become progressively thinner and subtler inland. Avoid heavy black outlines, sharp relief, mountainous ridges, thick roots, decorative symmetrical trees or isolated braided islands.
Output: full-bleed ORIGINAL photographic-style environmental illustration, no interface, labels, letters, logos, watermark, objects, boats, people, trees or vegetation. One single image. Do not introduce a sunset, orange accents, harsh directional light, horizon or extra landscape. The priority is the old image's quiet UI composition and colour continuity, with better lower-half creek detail, not a dramatic standalone landscape.
```
