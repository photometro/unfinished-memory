# Original generated environment assets

## Current environment — calm shore and paired opening / Study 16, 2026-10-05

Run UI-20261005-8-06 / #8 R5. Built-in image generation only: three local water retouches and three paired opening seas. Originals and all old versions remain. These are photographic-style generated illustrations, not measured terrain, photographs or scientific tides. Land/crop/channel composition was visually checked, not certified pixel-identical.

All seven new PNGs are 887×1774 in `public/environment/three-state-v2/`:

| Asset | Source / operation | SHA-256 |
|---|---|---|
| shore-daylight-source.png | Exact user-selected attachment 4b031b21; intentionally identical to historical V5 noon, regardless of filename | 25BC53C3E52855E302C11BA2C39F122A7B0E042ACAB1BDFEA07AC2F0C011A2BD |
| shore-daylight.png | Local lower-right water retouch of that attachment | 5ADA5490B9351E4DB90CBCE79772B08A4B7EE7EAF44F4E517F3303A8E0213801 |
| shore-slant.png | Local water retouch of three-state-v1/shore-slant.png | 004536717398B410D234DE42A5280DE5755D94A940AF0277A61A09CDD8EC10A1 |
| shore-night.png | Local water retouch of three-state-v1/shore-night.png | B1622D9E9E9AEA8A78B0BDE539D50EB5E84CD095F9EAB4E13DCD5F68D5A9AA1F |
| sea-daylight.png | Opening matched to new daylight shore / calm-sea reference | C92E908951CF96904F72A0C550A7DC321C42225E2CDFB2FD74AA13882745A62C |
| sea-slant.png | Opening matched to slant shore / calm-sea reference | 14676B7DF9771DA7D9790726D1E390E2E5A33D09BA3E58A5CCA0A172E996DC27 |
| sea-night.png | Opening matched to night shore / calm-sea reference | D0EE0BD851286C1F26C3E90C9EA444A83D951659F4E23F0AF65323F1AB9903F6 |

- Exact prompts and retained generator output paths: DAYLIGHT-PROMPTS Study16. The app reads only local copies. No runtime upload of memory content.
- Home remains unfiltered photographic layers with actual decoded dimensions and shared proportional A/B/C projection; no mounted homepage tide renderer/canvas/RAF. Buttons retain their glass; all full-frame home pseudo-element gradients are removed across the CSS cascade.
- Opening uses a source-photo-space distance field from the same daylight shore in all periods. High flats reveal before near-channel low terrain; only local alpha boundaries soften, layer opacity remains 1. This is an artistic reveal, not a DEM or hydraulic simulation. At progress 1 every pixel covers; at 0 every pixel clears. The component unmounts and detaches its MotionValue/visibility subscriptions after normal/skip/reduced entry.
- Shader/atlas/old sea assets remain dormant. No old lighting matrix is applied to the new photographic variants. The sea+geometry reference decode are admitted before entry with an 1800ms budget after shore readiness; late arrival cannot join that session. Missing sea degrades to the shore, missing replacement shore retains a prior good frame, cancelled callbacks cannot apply stale state.
- Each decoded photo estimates 6,294,152 RGBA bytes. The module cache retains at most six active source images (~36 MiB lower bound); exit only releases the entry canvas and callbacks, NOT all cached photos. Source mask 384×768, two canvas pixel stores plus field/mask arrays are additional. No heap/GPU/native performance or strict ceiling claimed.
- Functional colours, embedded font, fold geometry, paper boat and sealing current are protected and unchanged. Three-state visual acceptance remains with the user.

## Historical environment — three reviewed states / Study 15, 2026-10-05

No image generation, recolouring or bitmap editing in this Run. Independent copies of reviewed assets are used under `public/environment/three-state-v1/`; all older originals remain. They are generated photo-style artwork, not measured geography or real aerial photography. AI variants are not claimed to have pixel-identical terrain.

| Active state / time | Asset | Reviewed source | SHA-256 |
|---|---|---|---|
| 日光 / 06–14 | shore-daylight.png | environment-image-review/2026-10-05-master-review-v5/shore-morning.png | D50715D048EB8636A62C2DB9A68FD6A4844A58372E2738095727045A04B0FB04 |
| 斜阳 / 14–18 | shore-slant.png | environment-image-review/2026-10-05-light-separation-v6/shore-afternoon.png | E4DDFFD1464E0AB9CE6B1CF8EE387E589EF43B7D8B1772DAC1A6D113FB9F57DA |
| 夜色 / 18–06 | shore-night.png | environment-image-review/2026-10-05-master-review-v5/shore-night.png | 19DB2F3428D378E943B791AA2AE235C71A458C044579FC984B8CE798A34FE657 |

- All shore images are 887×1774. `shore-master.png`, SHA256 `41B3B1399939BC4277937363FCE11F7643E2E5B75BE776D045685EC6CDEF7AD5`, is for studio crop comparison, not a fourth active state. Noon artwork remains in review/history but is not active.
- Homepage FrozenSurface uses unfiltered photographic image layers and shared proportional cover/A-B-C rotation. No water replacement, terrain atlas, wet edge, homepage tide timer, canvas or WebGL is mounted in this branch. Historical renderer/helper files remain intact behind the paused default.
- Opening sea is still `environment/four-period/sea-noon.png`, SHA256 `C567A428A7DB905037F067B876A1B963BD2133B3C52969F4C6E51BE46E6A6CD7`. Only the sea layer uses the retained effective lighting matrix. Opening progress controls its opacity, not homepage water level; after entry this sea layer is removed.
- Shore/sea decode independently; unavailable sea cannot hide an available shore, cancelled loads cannot replace a newer state, and failed replacement shore retains the previous one. Browser network-delay/failure injection remains untested; controlled loader tests cover failures/cancellation.
- One decoded 887×1774 RGBA image estimates 6,294,152 bytes (~6 MiB). Switching may temporarily keep old/new image layers and the browser can cache prior images. This is a dimension-derived budget, not measured heap/GPU or native performance; no claim of a strict total memory ceiling.
- Functional colours, embedded font, fold geometry, paper boat and sealing current are unchanged. This Run does not regenerate the opening image or approve all A/B/C crops.

## Historical environment — selected v4 / Study 14, 2026-10-05

- The v4 master bitmap is unchanged, SHA256 `CE48CE2FDA157DCA443257773E3173A8DC53F9F4BC714936A485D239A8B93762`. No new bitmap is generated or edited this run.
- Opening sea texture restored to `public/environment/four-period/sea-noon.png`, SHA256 `C567A428A7DB905037F067B876A1B963BD2133B3C52969F4C6E51BE46E6A6CD7`. The earlier calm candidate stays on disk but is no longer referenced. The opening uses this texture under current lighting; the homepage retains original photo water instead of replacing it with the opening sea.
- `shore-terrain.js` produces a fixed source-coordinate artistic atlas: calibrated creek/estuary envelopes constrain raw photo-water samples; distances follow the photographed water boundaries, not idealized centerlines. Raised sand tongues and minimum connected-to-sea thresholds constrain inundation. The RGBA channels store quantized flood threshold, original water fraction, and bank-facing x/y normals. No screen-height threshold or rectangular UI no-flood zone. This is not a measured DEM, hydraulic model, or geographical tide prediction.
- The first centerline-based candidate was rejected in full UI review because it created thick tubular blue paths; it is not the shipped candidate. Connected bank geometry now derives from actual photo-water samples within the calibrated envelopes.

| Period | Brightness / saturation | RGB gain | Land / water lift | Contrast | Direction / beam | Beam tint / sheen |
|---|---|---|---|---:|---|---|
| Morning | 1.14 / 1 | 1.07 / 1.025 / .985 | .008 / .005 | 1.065 | −.88, −.47 / .24 | 1, .62, .20 / .012 |
| Noon | 1.19 / 1.04 | 1.02 / 1.01 / .985 | .004 / .004 | 1.08 | .10, −.995 / .065 | 1, .97, .90 / .015 |
| Afternoon | 1.055 / .97 | 1.105 / 1.035 / .915 | .005 / .005 | 1.085 | .89, −.46 / .26 | 1, .54, .14 / .013 |
| Night | .52 / .82 | .82 / .98 / 1.23 | .008 / .010 | 1.035 | −.71, −.70 / .10 | .25, .52, 1 / .017 |

- The full-image homepage pale overlay is removed. Local bank light/shadow, mild material lift and limited water sheen share the CPU/GL helper parameters. The 720ms lighting transition is retained; `data-light-settled` supports stable capture. Final aesthetics remain subject to user approval.
- Tide values stay .08/.30/.52. Daily auto tide is an illustrative 12h25m cycle independent of light; the former eight-minute loop is exposed only as an explicit workbench demo. Opening sea blend .57→.94 is separate from homepage tide. Existing photo water stays visible; new water develops around photo banks with source detail retained, ripple amplitude .00045, no added breaking waves. Wet banks are mild (.045) and dry independently of visual animation frames.
- `opening` explicitly resets wet history during normal, skipped and reduced entry. Static/reduced drying uses a bounded 0.5Hz task, suspended off-screen and cancelled on unmount. UI navigation, writing backgrounds, folded boat and persistent current are unchanged.
- One persistent environment, three 444×888 textures: estimated RGBA upload 4,731,264 bytes (~4.51 MiB); decoded source images 12,588,304 bytes (~12.01 MiB). One packed atlas cache (1,577,088 bytes) remains for backend switching; temporary flood arrays are not retained. Canvas buffers and CPU allocations are additional. These are dimension-derived budgets, not measured GPU/native memory.
- In this browser session first atlas preparation took about 876ms, cached switching 0–0.1ms. This remains a first-load performance limitation, not a frame-rate benchmark or Android result. CPU/GL atlas sampling uses NEAREST; static water motion and edge antialiasing differ intentionally, not pixel-perfect rendering.
- Evidence: `verification/UI-20261005-8-04/`, including 12 WebGL and 8 CPU states, intro endpoints and measured container bounds. The preview/crop has host zoom differences; DOM bounds, not image crop size, prove responsiveness. All original images and historical evidence remain.

## Historical environment — selected v4 / Study 13, 2026-10-04

- Shore master is unchanged: `public/environment/candidates/shore-master-v4.png`, 887×1774, SHA256 `CE48CE2FDA157DCA443257773E3173A8DC53F9F4BC714936A485D239A8B93762`. Time changes never regenerate or reshape the terrain.
- Water now uses the original generated calm-estuary candidate `public/environment/candidates/sea-calm-v1.png`, 887×1774, SHA256 `0EC78183315B6B76F2FAAE827DA5CBD0E9F84494BBA4D8923F7DEB16493C62C5`. Generated original: `<local-generated-images>/exec-166d388d-7412-4866-bdf8-81b9a1b8b59f.png`. Exact prompt is in DAYLIGHT-PROMPTS.md. No reference-photo pixels were used; this is photographic-style generated artwork, not measured water or terrain. The old sea-noon and all previous candidates remain unchanged.
- Four controlled light fields share the same source pair. They combine a color matrix, separate land/water brightness lift, contrast, broad directional illumination and a restrained water sheen. This is artistic relighting, not astronomical sun or weather simulation.

| Period | Brightness / saturation | RGB gain | Land / water lift | Contrast | Direction / beam | Beam tint / sheen |
|---|---|---|---|---:|---|---|
| Morning | 1.07 / .96 | 1.055 / 1.025 / .975 | .105 / .035 | .96 | −.82, −.57 / .065 | 1, .62, .23 / .008 |
| Noon | 1.11 / 1.03 | 1.005 / 1.018 / 1.035 | .135 / .025 | 1.025 | .15, −.99 / .025 | .88, .95, 1 / .017 |
| Afternoon | 1.015 / .98 | 1.095 / 1.015 / .895 | .06 / .02 | 1.015 | .87, −.49 / .12 | 1, .51, .15 / .012 |
| Night | .44 / .78 | .76 / 1 / 1.24 | .022 / .035 | 1 | −.62, −.78 / .035 | .22, .55, 1 / .012 |

- The 19 values blend from their current state over 720ms, retaining interruption behavior. Noon is no longer an identity matrix. Reduced/inactive scenes use the target directly. Preview SVG thumbnails show basic matrix color only and are explicitly labelled **not final rendered lighting**.
- `environment-material.js` is the shared CPU reference and GLSL parameter source. Channel classification samples raw, unlit shore color. An illustrative source-space height function makes low/mid/high (.08/.30/.52) cover different areas; a .075 advance band darkens wet sand slightly. This is not a DEM or tidal prediction. High tide submerges fine branches while retaining some submerged trunk trace. Intro reaches full sea; its endpoint is the then-current homepage tide.
- Existing source water is also replaced predominantly by the calm candidate; changing only the new flood region would leave the old large waves visible. Calm water detail is .52 and WebGL refraction amplitude .00065. A .10 channel-depth shade preserves underwater traces and fades completely at full-sea intro. Geometry does not change with lighting.
- Centered cover mapping and A/B/C transforms are unchanged. Preparation proportionally crops source images to 444×888. One renderer and two textures remain: uploaded RGBA estimate 3,154,176 bytes (~3.01 MiB); original decoded-source estimate 12,588,304 bytes (~12.01 MiB). CPU buffers/framebuffers are additional. These are dimension-derived budgets, not measured heap/GPU/native memory.
- Actual-browser evidence: all four × three tides in WebGL, and all four × low/high in forced CPU fallback. Images and runtime metadata: `verification/environment-study-13/`. Visual agreement is checked, not a pixel-exact GPU/CPU assertion. Static recomposition is throttled to 10Hz; actual frame-time/long-task profiling remains unperformed.

## Historical environment — selected v4 / Study 12, 2026-10-04

- User-selected attachment and `public/environment/candidates/shore-master-v4.png` are byte-identical, SHA256 `CE48CE2FDA157DCA443257773E3173A8DC53F9F4BC714936A485D239A8B93762`, 887×1774. This is generated photo-style artwork, not measured geography or a real aerial photograph. Earlier provenance below is retained.
- Runtime source pair: this v4 shore and existing `public/environment/four-period/sea-noon.png` (887×1774). No new raster image generation or editing in this implementation. All old eight images and candidate versions remain unchanged.
- Four controlled code-native light variants in `src/environment-state.js`, used by GLSL and CPU alike:

| Period | Brightness | Saturation | RGB gain |
|---|---:|---:|---|
| Morning | 1.02 | .84 | .98 / 1.01 / 1.045 |
| Noon | 1.00 | 1.00 | 1 / 1 / 1 |
| Afternoon | .94 | .90 | 1.07 / 1.015 / .925 |
| Night | .38 | .64 | .86 / 1.025 / 1.16 |

- Noon is the identity matrix. Interrupted changes blend from their then-current matrix in 720ms; reduced or inactive scenes set the target without background blending. No light parameter modifies the shore's geometry.
- Shared centered cover anchors: source center (.5,.5), A 0°, B −90°, C 180°. Compute crop from actual bounds and 887/1774 aspect; no fixed .4733 or CSS-only rotation. Tide mask is a source-space illustrative height approximation, NOT DEM data. Artistic .08–.52 tide retains submerged creek traces; intro alone reaches full sea.
- Observed runtime: one environment instance, two 444×888 texture uploads, original loaded dimensions above. Raw decoded source lower-bound estimate 12,588,304 bytes (~12.01 MiB); two texture estimate 3,154,176 bytes (~3.01 MiB), zero GPU textures in CPU fallback. CPU pixel/height/composite buffers and framebuffers are additional. These are dimension-derived budgets, not measured browser heap/GPU/Android memory. Render target caps DPR 1.25 and approximately one million pixels; cache is instance-owned and released on cleanup.
- Static fallback recomposites the same source-space tide mask and applies the same color matrix, then uses the same cover affine. It is not a full-image sea opacity layer. Water movement/refraction is WebGL-only; CPU has static water, with tide/lighting transitions throttled. Historical fallback and fixed-time statements below no longer describe Study 12.

## Historical stage-one candidate — v2, 2026-10-04

- Current candidate: `public/environment/candidates/shore-master-v2.png` (887×1774, 2,339,022 bytes); generated by the built-in image tool as a local edit of the unchanged original `four-period/shore-noon.png`.
- Original generated output: `<local-generated-images>/exec-de439a04-9820-4fb6-be8d-2e86877b6c79.png`.
- User feedback: v1 made the creek more legible, but fit the homepage worse. v2 restores the original warm pale upper-half breathing room, trunk/estuary composition and lower-right water, while improving lower-half fine branching and shallow banks.
- The user's second photograph is structural reference only, not an edit target or copied photograph. This remains generated photo-style artwork, not measured geography or physics. Generation does not guarantee unchanged pixels; judge the result in the existing interface.
- v1 and all eight baseline textures remain on disk unchanged. Candidate mode now points to v2; existing original/candidate switch, A/B/C layout and static crop preview are reused. No new time derivatives, scheduling, tide or UV adaptation.
- User visual approval remains pending. Exact edit prompt follows in DAYLIGHT-PROMPTS.md; earlier entries below are provenance history.

Generated with the built-in image generation tool, 2026-10-02. These are original environment illustrations intended to feel photographic, not photographs or authoritative tide simulations. No reference-photo pixels, UI text, logos or watermarks were embedded. PNGs are copied into this prototype and the generated originals are preserved.

- `public/environment/shore-original.png`: dry/wet tidal flat and creek material.
- `public/environment/sea-original.png`: overhead water texture.
- `src/water-shader.js`: illustrative refraction, water-level mask, wet edge and subdued changing light.

## Shore prompt

Use case: photorealistic-natural. Asset type: ORIGINAL environment texture for an interactive mobile memory app, NOT a UI mockup. Create a photorealistic straight-down aerial view of a pale coastal tidal flat after ebb, portrait composition approximately 1:2. The upper 52 percent is quiet, expansive warm ivory-grey sand with extremely subtle wet sediment texture, deliberately free of detailed channels to accommodate UI text later. In the lower half, a naturally eroded dendritic tidal creek network fans from land at middle-left into a broad shallow grey-blue sea at bottom-right. One broad winding water-filled main creek, organically tapering tributaries and dense finer fern-like drainage tributaries; irregular recessed mud banks with restrained shadows, fine rippled silt and damp sediment, small pools. It must clearly read as natural top-down tidal geomorphology, NOT a botanical tree, dead branches, roots or a diagram. Muted blue-grey water, warm pale sand, soft diffuse overcast light, believable real small-scale surface detail, gentle contrast, no dramatic orange/black. The entire image is natural terrain; no horizon, no vegetation, no boats, no buildings, no people, no stones, no text, no UI, no logo, no watermark. Editorial drone-photography realism, calm and understated. Do not reproduce any reference photo composition exactly.

## Sea prompt

Use case: photorealistic-natural. Asset type: ORIGINAL natural ocean surface texture for an interactive mobile UI background, NOT a UI mockup. Create a photorealistic straight-down overhead view of quiet coastal seawater, portrait composition approximately 1:2. Fill the frame with muted misty blue-grey water and softly visible natural current bands that bend and merge gently, fine overlapping capillary wavelets and shallow elongated ripples. Slight variations in depth from slate blue-grey upper left to translucent pale blue-grey lower right. A few subtle diffuse silvery reflections, no glaring sun. Natural fluid patterns, not parallel drawn lines, not a contour map, not stylized vector art. Soft overcast coastal daylight, calm believable water texture, restrained saturation and medium-soft contrast. No coastline, no sand, no visible horizon, no waves breaking, no big foam, no land, no animals, no floating objects, no people, no boats, no text, no UI, no logo or watermark. The image will be used as an original water texture beneath subtle programmatic motion.

## Boundaries

No external image host is contacted by the running prototype. The runtime never uploads input. Static fallback cross-fades original textures rather than pretending to run the water shader. This does not prove native Android performance.

## Four-period extension — 2026-10-03

Eight project assets are saved in `public/environment/four-period/`:

| Scene | Morning | Noon | Afternoon | Night |
| --- | --- | --- | --- | --- |
| Shore | `shore-morning.png` | `shore-noon.png` | `shore-afternoon.png` | `shore-night.png` |
| Sea | `sea-morning.png` | `sea-noon.png` | `sea-afternoon.png` | `sea-night.png` |

Noon assets are byte-for-byte copies of the two existing original textures. Six other assets are individual built-in imagegen edits of those originals, changing lighting only. Generated originals remain under the Codex generated-images directory; full final prompts and exact saved paths are recorded in `DAYLIGHT-PROMPTS.md`.

All three UI compositions reuse these same eight images. The shader's tide geometry always samples the noon shore master. Four period weights blend actual texture color while water level, scene state and route remain independent. Fine AI texture details are not guaranteed pixel-identical; material quality is still a user acceptance question.

No third near-shore scene, video, 3D scene, location/time permission, automatic scheduling, extra theme pack or memory algorithm is introduced. The glass controls are CSS/Web material approximations, not native system Liquid Glass. Manual and system reduced-motion behavior and static fallback remain available.

## Stage-one shore master candidate — 2026-10-04

- Candidate: `public/environment/candidates/shore-master-v1.png` (887×1774, 3,140,155 bytes).
- Generated original retained: `<local-generated-images>/exec-08f738a8-e61f-4aed-a7da-44fc90950cfb.png`.
- Structural reference: the user's second tidal-tree photo, solely to guide fine dendritic creek morphology. This is an original AI-generated photographic-style illustration, not an edited/copy photograph, measured terrain, or physical tide map.
- Built-in image generation; exact prompt in DAYLIGHT-PROMPTS.md. No external host at prototype runtime.
- Explicit candidate mode uses this single neutral texture as shore material and the same geometry reference. Existing sea images and all old eight assets remain unchanged.
- The duplicate shore texture slots are a compatibility path for the existing renderer, not four generated lighting variants. No clock, tide cycle, crop/UV or texture-memory optimization is claimed.
- Await user acceptance before producing derivatives or replacing old assets. A/B/C transforms and renderer fixed aspect ratio are still legacy prototype behavior.
