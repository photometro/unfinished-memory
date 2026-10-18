# B 潮岸 · 水色与入口 V3

日期：2026-10-10。状态：待用户评审的新候选，不是正式采用版本。

## 已批准的修改方向

用户批准：沿用 B 的简洁海沙构图，增加浅水/中段水流/深水层次，把海水与沙滩向约 55:45 调整；通过一处尚未完全显露的潮沟入口连接潮汐树首页及“记忆暂隐、以后显露”的产品意图。

授权范围仅限图标候选。保留旧稿；不替换 Android 图标，不修改开屏、首页、封存动效、业务逻辑或待办清单。

## 制作和筛选

使用 imagegen 技能的内置 edit 模式，没有 CLI/API 回退。

1. 以 b-quiet-shore-v2.png 为编辑目标，增加连续水色层次、调整海沙分布、加入一处与海面相连的短潮沟入口。
2. 初稿层次和入口已出现，但按粗略颜色采样，蓝色水域仍约 61.1%；蓝色也比上一版更鲜。初稿保存在 drafts/b3-first-pass.png，不作为本轮主候选。
3. 对初稿再做一次受控编辑，增加沙滩面积，并用上一版作色系参考。最终候选为 b-quiet-shore-v3.png。

## 自审和边界

- 浅水、中段、深水通过较大的水色变化区分；不加入白色泡沫、细碎水纹或附加图形。
- 岸线新增一个短入口，末端浅化；没有完整树形或支流。这是设计意图，不是对真实潮沟形成或品牌识别效果的科学证明。
- 最终候选蓝色区域粗略占 56.6%，其余沙色及混合过渡约 43.4%，已接近55:45试稿目标。上一版同口径约64:36。
- 统计方法：完整方图每5像素采样一次，以 B-R>8 判定“水色”。这是颜色分区近似，不是语义分割、精确海岸面积或圆形 Launcher 裁切后的比例。它只用于判断占比变化。
- 生成保留同色系，但新稿蓝色仍比旧稿饱和，水色过渡也有更明显的分层。不能承诺原稿逐像素或精确色值保持；是否过强仍需用户评审。
- 已检查原图和对比图。48px下潮沟只是小转折，不是能清楚阅读的地貌细节；不能据此宣布已建立独一无二的品牌识别。
- 本轮没有添加名称、字标、纸船、太阳、月亮、徽章或额外启动品牌页。

## 交付

- b-quiet-shore-v3.png：1254×1254 RGB、完整方图候选。
- review-b-shore-v3.png：上一版与新稿对比；330px圆角方图、48/64px小图和96px圆形裁切。
- render-shore-review-v3.ps1：仅评审排版、等比缩放与遮罩，不编辑候选原图。
- drafts/b3-first-pass.png：第一遍试稿。

候选及对比图不是正式 Android adaptive foreground/background/monochrome 图层，也不是真机截图。已安装图标未动。后续选择应由用户确认。

## 文件完整性

- 原始 B：3476249E39F8A1AE04CC2D667D5448F383DC45F5C600DD4F6CDA3182CEBE96A9，保持不变。
- 上一版 B2：C1073F77ACF8811F5EE495738BA309E042A67043FBCA5DAB68C05C5E8EA8600E，保持不变。
- 本轮 B3：56EFFC324C48DFDD877139B250AEA80E8DC90D12D3DD2BE5A313DB956E10B9D6。

## 输入与生成来源

第一遍编辑目标：`<project-root>/design/icon-options/tidal-20261010-v3/b-quiet-shore-v2.png`。

初稿生成来源：`<local-generated-images>\exec-dbf0fb4c-32fb-44a3-b97a-fb7dc9429464.png`。

第二遍：初稿为编辑目标、上一版仅作色系参考。

最终生成来源：`<local-generated-images>\exec-b549cad7-08ba-4128-b019-ab5b755506b7.png`。

## 完整提示词 · 第一遍

```text
Use case: precise-object-edit.
Asset type: refinement B3 of the supplied Quiet Shore launcher icon for Unfinished, a quiet personal-memory app.
Input image 1 is the EDIT TARGET. Keep this existing design recognisable, not a new unrelated icon.
Primary request: make this shore icon feel like a softly flowing tidal coast, with more water depth, better water/sand balance, and ONE small tidal inlet that is only partly revealed.
Keep unchanged: full-bleed square image; warm clean ivory sand above; clear luminous sea-blue below; airy restrained illustration aesthetic; calm open space; smooth organic contours; no floating logo. Preserve the existing palette closely, without brighter cyan saturation or dramatic lighting.
Change 1, area balance: gently increase sand area so the complete image reads as approximately 45 percent sand and 55 percent water. Shift the main shoreline bend toward the optical centre, keeping a relaxed asymmetrical diagonal coastline from upper-left-middle to lower-right-middle, never horizontal halves. Treat the percentage as visual composition, not text. Leave ample clean sand above.
Change 2, water layering: add subtle shallow-water depth and two broad low flowing undulations in the water. Near shore: pale translucent aqua with an irregular-width transition. Middle: the original clear sea-blue, with ONE graceful broad darker current gently sweeping through it. Lower/deeper water: a slightly deeper blue blended into the middle. The current and gentle swells should read as naturally continuous watery colour-depth, not parallel ornamental bands, raised ribbons, foam stripes or paper layers. Use gentle light diffusion and subtle low relief only, without a shiny 3D wave. Clear but quiet large-scale shape and tone variations visible even at 48px. No fine wave texture.
Change 3, distinctive shoreline: at ONE section of the central coast, the blue water enters the sand as a short broad softly curving tidal inlet, connected directly to the sea, tapering inland and becoming paler until it disappears into the shallow sand-water transition. This is a single partly revealed tidal-channel MOUTH, not a complete tidal tree. It should be only a modest notch in the main shoreline, not a huge river cutting the icon in half. One controlled soft asymmetry, no branching tributaries, no ring, no island, no S-shaped sticker. Its incomplete visible extent should feel quietly emerging from beneath the tide, not drawn with a sharp cut-off or deliberate crack.
Composition: the shoreline remains the main identifiable contour and the icon still reads as two generous land/water masses. The inlet is integrated into that silhouette, not another added symbol. At thumbnail size the large coastline shape is clear.
Avoid: boat, folded paper, characters, letters, text, tiny dots, sun or moon, memory pictograms, complete tree, fine branches, marsh map, photographic landscape, horizon, storm waves, large surf curl, white foam, jewels, glass orb, thick bevel, sculptural folds, multiple equally spaced ribbon waves, knotted currents, infinity sign, muddy gray, dark dramatic lighting, extra props, a border or baked rounded corners, external shadow, presentation board or watermark.
Output: ONE finished square candidate icon, not a comparison board.
```

## 完整提示词 · 最终调整

```text
Use case: precise-object-edit.
Asset type: final controlled adjustment to the Unfinished B3 shore icon.
Input image 1 is the EDIT TARGET (new layered-water icon with the small partly revealed tidal inlet).
Input image 2 is ONLY the PALETTE reference (the preceding quieter blue / warm-ivory icon); do not restore its simpler shoreline shape.
Make exactly this final adjustment: expand the sand area modestly and return the water to the quieter sea-blue of image 2.
Area target: sand approximately 45% of the whole square, water approximately 55%. Image 1 has too much water. Move the main coastline downward by roughly 6 to 8 percent of canvas height, without rotating, stretching the shapes or changing their character. Broad visual placement: the shoreline meets the left edge near 25% height, passes through the central region and exits the right edge near 68% height; the mean sand area should approach45%. Preserve the graceful diagonal curvature and the existing small partly revealed inlet, moving them together. Do not create horizontal half-and-half blocks. Preserve ample clean sand and optical balance.
Palette adjustment: use the more restrained blue-teal water hue of image 2, with a modestly darker blue-teal depth region, not electric cyan or saturated cobalt. Preserve image 1's broad gentle wave/current undulations and its three connected water-depth areas, but reduce their colour contrast slightly so they feel like soft continuous water. No hard parallel strips.
Keep invariant: image 1's small softly fading inlet integrated into the coastline; no extra branches; warm clean ivory sand, quietly translucent shallow aqua transition; the complete square and calm semi-abstract style. Keep the water layers, current direction and shore geometry recognisable. Do not replace the image with a new design.
Avoid: extra symbols, new islands, complete tidal tree, branches, foam, small waves, circles, letters, boat, bright high-gloss rim, dark shadow, sculptural ribbons, texture clutter, background scenery, text, watermark, new props, borders or rounded corner mask.
Deliver ONE complete refined square icon only.
```

