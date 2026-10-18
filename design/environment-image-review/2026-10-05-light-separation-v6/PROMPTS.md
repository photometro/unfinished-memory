# V6 中午与下午实际生成提示词

生成方式：内置 imagegen；transparent_background=false。未使用 CLI/API 回退。

## noon

输入图片（顺序对应 Image 1 等）：

- <project-root>\design\environment-image-review\2026-10-05-master-review-v5\shore-master.png

```text
Use case: lighting-weather.
Asset: one photorealistic vertical 1:2 full-bleed aerial tidal-flat image for a calm memory app, NO UI.
Image 1 is the exact composition edit target. Edit illumination ONLY. Preserve its camera, full framing, upper quiet sand zone, ALL dendritic creek silhouettes, trunk position, sand tongues, exact shoreline and fixed tide level. The main creek remains connected to the lower-right estuary. Keep original fine shallow sand relief, delicate tiny ripple scale and photographic naturalness, never add tall dunes, terrain, foam or large wave bands. No sky, horizon, sun disk, objects, buildings, people, text, labels, borders or watermark.
The user says the noon and afternoon images do not separate clearly enough. Make the intended time legible at small mobile thumbnail size through real light direction and spatial highlights/shadows, NOT a uniform color overlay. Do not redraw the place or make it an exaggerated dramatic landscape.
Lighting: unmistakably clean midday high-sun light. Nearly overhead WHITE daylight, short subtle bank shadows, even bright pearly ivory sand, crisp daylight clarity. Restrained cool pale aqua/cyan water, cooler and fresher than warm afternoon water. No golden highlights, orange warmth, amber cast, sunset reflection or long diagonal shadows. Remove the low-angle shading character; keep bank relief readable with short blue-grey occlusion shadows. Very luminous, serene, detailed, not foggy, not blown white, not oversaturated tropical turquoise. The midday cue must be clear neutral pearl sand + cool fresh water + uniformly open high-sun illumination.
```

## afternoon

输入图片（顺序对应 Image 1 等）：

- <project-root>\design\environment-image-review\2026-10-05-master-review-v5\shore-master.png
- <project-root>\design\environment-image-review\2026-10-05-master-review-v5\shore-noon.png
- <project-root>\design\environment-image-review\2026-10-05-master-review-v5\shore-afternoon.png

```text
Use case: lighting-weather.
Asset: one photorealistic vertical 1:2 full-bleed aerial tidal-flat image for a calm memory app, NO UI.
Image 1 is the exact composition edit target. Edit illumination ONLY. Preserve its camera, full framing, upper quiet sand zone, ALL dendritic creek silhouettes, trunk position, sand tongues, exact shoreline and fixed tide level. The main creek remains connected to the lower-right estuary. Keep original fine shallow sand relief, delicate tiny ripple scale and photographic naturalness, never add tall dunes, terrain, foam or large wave bands. No sky, horizon, sun disk, objects, buildings, people, text, labels, borders or watermark.
The user says the noon and afternoon images do not separate clearly enough. Make the intended time legible at small mobile thumbnail size through real light direction and spatial highlights/shadows, NOT a uniform color overlay. Do not redraw the place or make it an exaggerated dramatic landscape.
Image 2 is the previous NOON comparison reference, do not copy its lighting.
Image 3 is the rejected prior AFTERNOON lighting; it was too similar to noon. Make a NOTICEABLY STRONGER difference than Image 3 while holding Image 1 geometry.
Lighting: distinctly LATE AFTERNOON warm low-angle sun from the upper-right. Honey/soft peach sunlight grazes existing sand and creek lips, longer but delicate cool slate shadows directed lower-left, showing the same tiny bank relief. Use visibly warm lit sand against cooler gently shaded patches, not a flat amber tint. Keep upper sand readable and warmly luminous, with a gentle spatial falloff leftward. Existing lower-right water has warm elongated low-sun reflections from small ripples near the right side, deeper muted blue-green elsewhere; restrained metallic gold glints, NO invented new wave shapes, no bright solid light streak. The image must read at thumbnail as warm raking evening-side sunlight, distinctly lower-angle and richer than high-sun white noon, yet retain airy photographic elegance. No muddy brown darkness, no heavy orange wash, no theatrical giant shadows.
```

