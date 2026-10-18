# 退潮湿滩母图 v4 — candidate generation record

- Date: 2026-10-04 (Asia/Shanghai).
- Mode: built-in image generation, precise-object-edit, opaque portrait.
- User-approved direction: recently exposed damp mudflat, coherent landward-tapering tidal drainage, quiet upper text region, identifiable middle-lower tidal tree and calm lower water.
- Initial edit target: `public/environment/candidates/shore-master-v2.png`, not v3.
- Structural reference: user's second tidal-tree photo. Layout-only reference: `verification/shore-v2/home-v2.jpg`; no UI baked into the asset.
- First draft retained at `<local-generated-images>/exec-2a7f8c70-b8e2-4b79-86b8-c85b2468cb40.png`.
- A targeted material/relief refinement followed after inspection of the first draft.
- Selected candidate original: `<local-generated-images>/exec-a584d0df-b4d9-433b-ad35-410e854636ca.png`.
- Project copy: `public/environment/candidates/shore-master-v4.png`.
- All older assets retained. No production asset replacement, consuming code changes, backlog updates, four-period derivatives, tide implementation, native changes or publishing.

## Review boundary

The candidate keeps the broad trunk and estuary composition and adds upper damp-silt tonal variation and connected peripheral drainage. The output still has stylized fine texture and some embossed-looking bank relief: do not treat the requested flatness as fully verified. Exact pixel identity of the lower landscape is not guaranteed. This is an AI-generated artistic simulation, not a measured geographical reconstruction or physics model.

The image was visually inspected, opened for dimension checks, and copied without overwriting older versions. No homepage with v4, encounter/quiet state comparison, text contrast, device crop, night/day consistency or animated tide was verified. The code continues to point to v2. User visual approval is pending.

## Initial exact prompt

```text
Use case: precise-object-edit.
Asset type: ONE clean portrait environmental master image for the realistic Unfinished mobile homepage, NOT a UI mockup.

Inputs:
Image 1 is the EDIT TARGET, shore-master-v2. Preserve its straight-down aerial viewpoint, portrait 1:2 framing, broad main-creek course and estuary position, soft muted warm-grey / mist-blue palette.
Image 2 is a GEOMORPHOLOGY REFERENCE ONLY: use its low-relief wet tidal flat and fine tapering drainage morphology. Do not copy its pixels, horizontal composition, colours, bright reflection, signature or watermark.
Image 3 is a LAYOUT REFERENCE ONLY: the mobile homepage is the tall phone on the right. Its small header, upper text block, central tidal tree, and bottom buttons define where visual calm is needed. Ignore the surrounding studio. Do NOT render any interface, text, phone frame, buttons or paper boat into the output.

Primary request: refine image 1 into a credible RECENTLY EXPOSED, STILL-DAMP tidal mudflat after ebb, with a quiet text-compatible upper area that feels like real terrain rather than a blank beige board. Make the land substantially flatter and suppress the chunky embossed sand-bank/canyon look. Preserve the overall scene and established drainage course, not a different coast.

Natural logic: nearly planar silty mudflat, shallow recessed channels, small low banks; a coherent hierarchy of hairline landward drainage feeding smaller creeks, joining the broader trunk and then the bottom-right coastal water. Branches become thinner and shallower inland, irregular and unsymmetrical. Water is visibly concentrated in the trunk, channels and a FEW shallow connected low spots. Most upper ground is exposed damp silt, not a submerged ocean, not a sheet of rippling blue paint. No ridge mountains, dunes, botanical roots, dry cracking patterns or isolated decorative veins.

Upper surface: replace uniform ivory sand with broad, softly irregular damp-silt tonal variations, subtle sediment staining aligned with local drainage, and sparse faint wet sheen in low spots. Predominantly matte pale warm-grey mud with delicate grain; NO dense water-wave texture or embossed wrinkles across the land. Keep approximately x 8–90%, y 15–42% low-contrast and calm for the existing headline and potentially longer arrival message. This area must retain believable surface material, NOT be airbrushed blank, fogged out or given a rectangular light patch. Allow just a few finely tapering drainage fingers around the outer edges, connected to the middle drainage network, avoiding dark lines under the text block.

Middle and lower composition: the recognizable dendritic tidal tree is the strongest landscape detail in the middle-lower frame. Keep its main course, but lower the bank relief and heavy shadows so it reads as a flat tidal wetland rather than miniature carved mountains. Blend upper silt and middle branches organically with no horizontal division. Retain generous open mist-blue-grey water toward the bottom-right, with soft broad surface variation and very restrained tiny ripples, no foam, glare or dense white marks. Reserve quiet water around the bottom action region and the future vessel landing space, without drawing the vessel.

Light and finish: soft diffuse neutral daytime, photographic aerial realism with restrained contrast and the target's gentle palette; subtle moisture darkening, not saturated turquoise or tropical beach blue. No dramatic sunset, bright sun reflection, haze blanket or strong directional shadows. The terrain remains readable without an opaque UI overlay. Preserve practical crop margins around the main trunk and estuary.
Output: ONE original full-bleed opaque portrait 1:2 background image only. No words, letters, logos, watermark, labels, border, collage, interface, mockup, people, vegetation, horizon, rocks, shells, props or boats. The priority is believable flat drainage plus homepage reading hierarchy, not maximum texture or a dramatic standalone landscape.
```

## Refinement exact prompt

```text
Use case: precise-object-edit.
Edit ONLY the land material and apparent relief in this image. Preserve the portrait 1:2 aerial composition, exact broad creek paths, main blue-grey trunk, estuary, coastline placement, bottom-right open water, branch hierarchy and muted colour palette. No UI or text.

The land currently looks like embossed clay, wrinkled sand ridges and miniature canyon banks. Replace that material with a genuinely nearly planar, recently exposed wet silty MUDFLAT. Remove the raised lobes, tiny ridges, dense crumpled texture and dark sculpted bank shadows. The banks should be low soft lips, channels shallow incisions into a flat continuous platform, not relief sculpture. Keep the slender dark-blue-grey dendritic channels sharp enough to read; don't erase their connections or make a blurry image.

Across the upper 40% especially, use softly irregular broad damp-silt tonal patches, delicate MATTE sediment grain and just a few tiny low-contrast residual wet spots connected to drainage. The existing upper-centre headline area must be quietly readable; no repetitive water waves, embossed wrinkle noise, dry cracks, glitter or opaque haze. Keep subtle natural tonal variety so it isn't a blank beige rectangle. The mid-lower creek tree stays the strongest landscape detail. Flatness should come from reducing physical relief and shadow, not covering the landscape in white paint or overexposing it.

Photorealistic flat coastal mudflat from directly overhead in soft diffuse neutral daylight; pale warm-grey silt, mist-blue-grey water, believable damp surfaces. Calm and low-contrast, without plastic smoothness. Do NOT add clouds, sunlight reflections, vegetation, dunes, rocks, boats, labels or any other objects. Output ONE clean original opaque portrait background only.
```

