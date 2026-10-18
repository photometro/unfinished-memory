# 环境素材提示词与版本来源


## 原型 16 / #8 R5 — 六张局部编辑与配套海面（2026-10-05）

内置 imagegen，一次调用一张目标，transparent_background=false。每张结果单独人工查看；提示词的地貌保持约束不等于像素一致保证。原图和生成原始输出均保留，具体 SHA 见 ASSETS Study16。以下为最终调用原文，不做后期位图调色。所有生成输出目录：`<local-generated-images>/`；项目副本：`public/environment/three-state-v2/`。

### shore-daylight.png

- References: User attachment 4b031b21
- Retained generator output: exec-abb31002-cf7e-4990-aedd-9369456f8732.png

```text
Edit this exact portrait background locally for the Unfinished memory app. Keep the image dimensions/aspect, crop, nadir aerial viewpoint and EVERY sandbank, tidal-tree branch, creek, shoreline, land texture, daylight brightness, ivory-sand and cyan-blue colors unchanged. Only calm the broad open water in the lower-right quadrant and along the bottom right: remove the strong corrugated wave ridges and dense glitter streaks. Replace them with nearly still shallow estuary water, the SAME water color and clear depth gradients, only delicate very low-contrast fine ripples, soft continuous reflected daylight. Do not flatten or repaint any tidal channels or land. No foam, breaking waves, horizon, vignetting, new shadows, haze or added objects. This is an extremely restrained, invisible local water retouch, NOT a redesign or global regrade. Preserve the readable quiet sand area at the top. No UI or text. Save this as a standalone final bitmap suitable for a mobile app background.
```

### shore-slant.png

- References: three-state-v1/shore-slant.png
- Retained generator output: exec-b9a852e4-fa55-429d-a603-a8a31999f93f.png

```text
Extremely restrained LOCAL RETOUCH of this exact portrait mobile-app estuary background. Preserve dimensions and crop, nadir viewpoint, every sandbank, tidal-tree branch, creek, coastline, land texture, all existing light and color: the existing golden low-angle light, cool blue-grey sand shadows and restrained warm water reflections. Modify ONLY the broad open-water area at bottom right and its lower-edge continuation. Remove the large corrugated wave ridges and dense glitter streaks; make nearly still estuary water in the SAME color and depth gradients, only tiny soft low-contrast ripples. Do not redraw channels, land, geometry, shoreline or globally recolor/brighten/darken. No foam, surf, horizon, new light sources, vignette, haze, objects, text or UI. Maintain the top quiet readable sand area. Invisible water cleanup, not a new landscape.
```

### shore-night.png

- References: three-state-v1/shore-night.png
- Retained generator output: exec-5616ece5-efac-4e96-80cb-368a0c9d2ccc.png

```text
Extremely restrained LOCAL RETOUCH of this exact portrait mobile-app estuary background. Preserve dimensions and crop, nadir viewpoint, every sandbank, tidal-tree branch, creek, coastline, land texture, all existing light and color: the existing luminous silver-blue night, readable cool sand details and subtle soft moonlight on the water. Modify ONLY the broad open-water area at bottom right and its lower-edge continuation. Remove the large corrugated wave ridges and dense glitter streaks; make nearly still estuary water in the SAME color and depth gradients, only tiny soft low-contrast ripples. Do not redraw channels, land, geometry, shoreline or globally recolor/brighten/darken. No foam, surf, horizon, new light sources, vignette, haze, objects, text or UI. Maintain the top quiet readable sand area. Invisible water cleanup, not a new landscape.
```

### sea-daylight.png

- References: new shore-daylight + candidates/sea-calm-v1
- Retained generator output: exec-24a91179-1de2-4ce8-9765-cecf51359027.png

```text
Create a portrait 1:2 opening-sea background for the same mobile app and estuary as reference 1. Strict straight-down aerial view of ONLY calm blue-cyan tidal estuary water covering the full frame. Use reference 1's lower-right water as the exact palette/material/daylight reference: fresh muted turquoise cyan, bright but not blown out, soft even natural daylight with one coherent subtle illumination direction. Reference 2 is only the calmness reference; reduce its dense glittering ripples substantially. Tiny gentle low-amplitude ripples, almost still open water, photographic delicate real water depth, no flat vector fill. No horizon, sand, shoreline, land, foam, large wave ridges, caustic nets, glare stripe, spotlight, artificial vignette, dramatic dark corners or mottled random light. Top/upper-left enough continuous darker blue-cyan for small white opening copy without a separate dark rectangle, but a very gradual believable water-depth variation across the image, not a dark UI mask. Quiet, transparent, restrained, coherent with reference 1. No objects, text or UI. Save standalone background.
```

### sea-slant.png

- References: new shore-slant + candidates/sea-calm-v1
- Retained generator output: exec-8b9643f9-951b-4156-99ce-c90dd4851841.png

```text
Portrait 1:2 photo-realistic mobile-app opening background, straight-down aerial view of ONLY calm estuary sea covering the entire frame. Same location as reference 1. Match its lower-right water material and illumination: muted blue-teal water with restrained soft golden light coming from the upper right, cool blue shadows; gradual gentle warm reflection near the right edge only, NO blazing orange glare. Reference 2 ONLY guides delicate calm ripples; reduce its busy sparkles substantially. Nearly still shallow tidal-estuary water, very fine low-amplitude low-contrast ripples, soft coherent natural light from ONE direction, tiny smooth depth gradients, no harsh patches of random lighting. NO horizon, land, shoreline, sand, objects, foam, surfing waves, corrugated large wave ridges, caustic nets, strong vignette or spotlight. Full image must feel coherent with reference 1, not another ocean or a flat blue paint. Upper-left copy area stays subtly quiet/darker by natural water depth, not an obvious UI mask. No text or UI.
```

### sea-night.png

- References: new shore-night + candidates/sea-calm-v1
- Retained generator output: exec-e22de69f-2362-427a-a0a7-3e26e039bf3b.png

```text
Portrait 1:2 photo-realistic mobile-app opening background, straight-down aerial view of ONLY calm estuary sea covering the entire frame. Same location as reference 1. Match its lower-right water material and illumination: deep silver-blue night water, soft low-contrast blue-silver reflections and readable dark navy texture; NO lights, stars, moon, glitter band, neon or bright silver streak. Reference 2 ONLY guides delicate calm ripples; reduce its busy sparkles substantially. Nearly still shallow tidal-estuary water, very fine low-amplitude low-contrast ripples, soft coherent natural light from ONE direction, tiny smooth depth gradients, no harsh patches of random lighting. NO horizon, land, shoreline, sand, objects, foam, surfing waves, corrugated large wave ridges, caustic nets, strong vignette or spotlight. Full image must feel coherent with reference 1, not another ocean or a flat blue paint. Upper-left copy area stays subtly quiet/darker by natural water depth, not an obvious UI mask. No text or UI.
```


## 原型 14 当前方式（2026-10-05）

本轮不生成或编辑位图。保留用户选定 v4 母图，开屏恢复原有 `four-period/sea-noon.png`；首页保留母图原潮沟水色，光影、岸线扩展和轻微水纹由同源坐标代码渲染。具体光照、材质、艺术潮汐和性能限制以 ASSETS.md Study 14 为准。下列生成提示词只是历史资产记录，不是本轮新生成结果。

## 原型 13 历史方式（2026-10-04）

v4 潮滩母图保持原位图及哈希不变。四时光照在同一母图上受控派生，不再各画一处潮滩。本轮只生成一张新的平静河口浅水素材，替换运行时海面基图，同时用于开屏和首页的原有水域／涨潮区域。旧图不覆盖，亮度、微波与水下层次在代码内调整；参数见 ASSETS.md。最终视觉仍待用户验收。

### 平静浅水候选 · sea-calm-v1

- 工具：内置 imagegen；新图生成，无参考图片，非透明背景。
- 项目文件：`public/environment/candidates/sea-calm-v1.png`，887×1774。
- 生成原件：`<local-generated-images>/exec-166d388d-7412-4866-bdf8-81b9a1b8b59f.png`。
- SHA256：`0EC78183315B6B76F2FAAE827DA5CBD0E9F84494BBA4D8923F7DEB16493C62C5`。

```text
Use case: photorealistic-natural. Asset type: original calm estuarine water texture for the existing Unfinished tidal-flat mobile app, NOT a UI mockup. Create a portrait 1:2 top-down aerial photographic-style image, ideally 887 by 1774 pixels, fully filled by very calm sheltered estuary water beside a shallow tidal flat. Water is pale muted grey-cyan / misty mineral blue, base around #98b4b7 to #abc1c1; luminous rather than dark slate. Real small-scale almost flat surface: fine very low-amplitude capillary ripples, delicate irregular wisps of diffuse daylight reflection, subtle variations of shallow translucent water with diffuse pale muddy bed below. Shot from a high drone, never a close-up of rolling ocean. Bright neutral diffuse daylight to allow the same image to be relit in code for morning/noon/afternoon/night. Soft tonal range and low contrast, natural photographic microtexture, not completely flat or vector. No broad wave crests, no chunky ridged waves, no rolling swells, no foam, no breaking surf, no dark troughs, no high contrast white highlights, no glare disc, no sun rays painted in, no caustic net, no ocean-current knots. No shore, no exposed land, no sky or horizon, no tree branches, no boats or props, no people, no buildings, no lettering, no watermark, no border. Keep all four corners consistent in water scale and brightness, with soft organic variation. This texture will cover existing water and softly flooding tidal-flat regions; it should blend with pale warm sand without dominating the UI.
```

## 历史 · 原型 12 方式（2026-10-04）

用户选择 v4 母图后，本次没有再生成四时段图片，也没有编辑原位图。采用同一 v4 潮滩与现有 sea-noon 基图，通过代码中的受控颜色矩阵派生四种光照；参数、来源、统一坐标和资源预算见 ASSETS.md。此做法保证地貌不随调光被模型重绘，静态／动态使用相同计算。下面提示词仅为旧八图的生成溯源，不再作为当前运行入口。

使用内置 imagegen 的编辑模式，2026-10-03。原图保留，项目副本位于 `public/environment/four-period/`。中午两图直接复制现有母图，没有再次生成。

## shore / morning

- 编辑母图：`<project-root>\design\tidal-realistic-prototype\public\environment\shore-original.png`
- 项目文件：`public/environment/four-period/shore-morning.png`
- 生成原件：`<local-generated-images>\exec-f8303713-7868-45a1-ac99-9eb2b20cea7b.png`

```text
Use case: lighting-weather. Input image 1 is the EDIT TARGET, the original generated tidal-flat creek landscape. Asset type: individual photorealistic-natural environment texture for a four-time-of-day interactive UI study, NOT a mockup or collage. Change ONLY light and color to morning. Soft cool mist-blue early morning with a very faint warm cream dawn light. Preserve subtle sediment texture and readable water detail. Restrained saturation, gentle diffuse illumination, no dense fog obscuring terrain. Preserve the EXACT tidal creek network and shoreline: every tributary, main creek, pools, bank contour, upper quiet sand area and lower creek position. Keep all waterways and land in precisely the same places. Do not change water level, geology or shapes. Preserve the original straight-down aerial camera, original portrait 1:2 framing, scale, crop, orientation and spatial registration. This is the SAME scene at a different time, not a new landscape. No text, UI, logos, watermarks, people, buildings, boats, plants, particles or extra objects. Keep believable natural textures and muted calm palette. Output ONE image filling the entire canvas.
```

## shore / afternoon

- 编辑母图：`<project-root>\design\tidal-realistic-prototype\public\environment\shore-original.png`
- 项目文件：`public/environment/four-period/shore-afternoon.png`
- 生成原件：`<local-generated-images>\exec-ba946a4b-bc02-479f-b1f1-76ce1afbe8ea.png`

```text
Use case: lighting-weather. Input image 1 is the EDIT TARGET, the original generated tidal-flat creek landscape. Asset type: individual photorealistic-natural environment texture for a four-time-of-day interactive UI study, NOT a mockup or collage. Change ONLY light and color to afternoon. Quiet late-afternoon light: subtle pale warm gold and warm grey reflected illumination, slightly longer gentle shadows. Restrained saturation. Not orange sunset, no dramatic contrast, no glaring sun reflection. Preserve the EXACT tidal creek network and shoreline: every tributary, main creek, pools, bank contour, upper quiet sand area and lower creek position. Keep all waterways and land in precisely the same places. Do not change water level, geology or shapes. Preserve the original straight-down aerial camera, original portrait 1:2 framing, scale, crop, orientation and spatial registration. This is the SAME scene at a different time, not a new landscape. No text, UI, logos, watermarks, people, buildings, boats, plants, particles or extra objects. Keep believable natural textures and muted calm palette. Output ONE image filling the entire canvas.
```

## shore / night

- 编辑母图：`<project-root>\design\tidal-realistic-prototype\public\environment\shore-original.png`
- 项目文件：`public/environment/four-period/shore-night.png`
- 生成原件：`<local-generated-images>\exec-251b42ac-3763-4c38-b01b-25ebfd858699.png`

```text
Use case: lighting-weather. Input image 1 is the EDIT TARGET, the original generated tidal-flat creek landscape. Asset type: individual photorealistic-natural environment texture for a four-time-of-day interactive UI study, NOT a mockup or collage. Change ONLY light and color to night. Natural moonlit evening, luminous slate and mist-blue grey. Preserve every creek and sediment detail with soft silver ambient reflected light. Clearly nighttime but exposed enough to read the terrain, not black, not neon blue. No stars or moon in frame. Preserve the EXACT tidal creek network and shoreline: every tributary, main creek, pools, bank contour, upper quiet sand area and lower creek position. Keep all waterways and land in precisely the same places. Do not change water level, geology or shapes. Preserve the original straight-down aerial camera, original portrait 1:2 framing, scale, crop, orientation and spatial registration. This is the SAME scene at a different time, not a new landscape. No text, UI, logos, watermarks, people, buildings, boats, plants, particles or extra objects. Keep believable natural textures and muted calm palette. Output ONE image filling the entire canvas.
```

## sea / morning

- 编辑母图：`<project-root>\design\tidal-realistic-prototype\public\environment\sea-original.png`
- 项目文件：`public/environment/four-period/sea-morning.png`
- 生成原件：`<local-generated-images>\exec-b22ac775-dc1e-448e-a580-f1db5c67376d.png`

```text
Use case: lighting-weather. Input image 1 is the EDIT TARGET, the original generated overhead seawater texture. Asset type: individual photorealistic-natural environment texture for a four-time-of-day interactive UI study, NOT a mockup or collage. Change ONLY light and color to morning. Soft cool mist-blue early morning with a very faint warm cream dawn light. Preserve subtle sediment texture and readable water detail. Restrained saturation, gentle diffuse illumination, no dense fog obscuring terrain. Preserve the same ocean ripple arrangement, broad current bands, camera position, framing and fine water texture. No coast, land, waves breaking or additional elements. Preserve the original straight-down aerial camera, original portrait 1:2 framing, scale, crop, orientation and spatial registration. This is the SAME scene at a different time, not a new landscape. No text, UI, logos, watermarks, people, buildings, boats, plants, particles or extra objects. Keep believable natural textures and muted calm palette. Output ONE image filling the entire canvas.
```

## sea / afternoon

- 编辑母图：`<project-root>\design\tidal-realistic-prototype\public\environment\sea-original.png`
- 项目文件：`public/environment/four-period/sea-afternoon.png`
- 生成原件：`<local-generated-images>\exec-2fdc5c6f-44d0-42f9-b79c-d9819840bc29.png`

```text
Use case: lighting-weather. Input image 1 is the EDIT TARGET, the original generated overhead seawater texture. Asset type: individual photorealistic-natural environment texture for a four-time-of-day interactive UI study, NOT a mockup or collage. Change ONLY light and color to afternoon. Quiet late-afternoon light: subtle pale warm gold and warm grey reflected illumination, slightly longer gentle shadows. Restrained saturation. Not orange sunset, no dramatic contrast, no glaring sun reflection. Preserve the same ocean ripple arrangement, broad current bands, camera position, framing and fine water texture. No coast, land, waves breaking or additional elements. Preserve the original straight-down aerial camera, original portrait 1:2 framing, scale, crop, orientation and spatial registration. This is the SAME scene at a different time, not a new landscape. No text, UI, logos, watermarks, people, buildings, boats, plants, particles or extra objects. Keep believable natural textures and muted calm palette. Output ONE image filling the entire canvas.
```

## sea / night

- 编辑母图：`<project-root>\design\tidal-realistic-prototype\public\environment\sea-original.png`
- 项目文件：`public/environment/four-period/sea-night.png`
- 生成原件：`<local-generated-images>\exec-92f81f3a-ef01-4532-b129-0076c8d3307e.png`

```text
Use case: lighting-weather. Input image 1 is the EDIT TARGET, the original generated overhead seawater texture. Asset type: individual photorealistic-natural environment texture for a four-time-of-day interactive UI study, NOT a mockup or collage. Change ONLY light and color to night. Natural moonlit evening, luminous slate and mist-blue grey. Preserve every creek and sediment detail with soft silver ambient reflected light. Clearly nighttime but exposed enough to read the terrain, not black, not neon blue. No stars or moon in frame. Preserve the same ocean ripple arrangement, broad current bands, camera position, framing and fine water texture. No coast, land, waves breaking or additional elements. Preserve the original straight-down aerial camera, original portrait 1:2 framing, scale, crop, orientation and spatial registration. This is the SAME scene at a different time, not a new landscape. No text, UI, logos, watermarks, people, buildings, boats, plants, particles or extra objects. Keep believable natural textures and muted calm palette. Output ONE image filling the entire canvas.
```

## 2026-10-04 — Stage-one neutral shore master v2 (current candidate)

Output: `public/environment/candidates/shore-master-v2.png`. Built-in image edit of the original noon shore, with the user's second photograph as structure-only reference. Exact inputs, constraints and full prompt: [SHORE-V2-PROMPT.md](SHORE-V2-PROMPT.md). The edit preserves warm upper-half breathing room and original trunk/estuary composition; only lower-half fine creek morphology is enhanced. v1 and original textures remain unchanged. No derivatives yet; still awaiting visual approval.

## 2026-10-04 — Stage-one neutral shore master v1 (retained, not current candidate)

Output: `public/environment/candidates/shore-master-v1.png`.

Original: `<local-generated-images>/exec-08f738a8-e61f-4aed-a7da-44fc90950cfb.png`.

Reference is structural guidance only, not an edit target. One candidate; no four-period derivatives yet.

```text
Use case: photorealistic-natural. Asset type: ONE original portrait tidal-flat environmental master texture for an interactive mobile memory app, not a UI mockup. The provided real photograph is a STRUCTURAL REFERENCE ONLY, not an edit target: study its slender, dense, hierarchically branching dendritic tidal-creek network on a nearly flat muddy intertidal surface. Do not reproduce its composition, watermark or pixels. Create a new straight-down aerial scene, portrait about 1:2. A distinct, modest-width main creek connects to quiet grey-blue coastal water in the lower right; tapering tributaries branch landward into dense, long, feather-like fine channels across the middle and lower half, with coherent drainage connectivity and naturally irregular branching. Show a shallow low-relief mudflat, smooth damp pale warm-grey silt with delicate sediment ripples and wet sheen. The channels are recessed delicate dark blue-grey lines with restrained shallow bank shadows, not thick raised roots, chunky islands, braided sand bars or steep canyon relief. Keep enough comparatively quiet land in the upper area for a small UI header, but not an empty featureless upper half: fine lower-contrast tributaries may extend upward. Main creek junction around the lower-middle of frame, spacious quiet water toward bottom right; useful terrain extends to all edges with crop safety for narrow and wide mobile windows. Neutral soft diffuse daylight, believable photographic surface detail, muted mist-blue water and pale sand-grey sediment, calm gentle contrast, no exaggerated orange, no dramatic sunset, no sharp raking light. No horizon, plants, tree trunks, branches above ground, buildings, roads, people, boats, stones, large foam, lettering, UI, logos, signatures or watermarks. One full-bleed original photo-like environmental image, no collage or presentation border.
```
