# 未完成图标重新设计 · V2

日期：2026-10-10。

## 为什么重新设计

用户否定上一轮全部候选。上一轮把潮滩、洋流直接缩为画面，或把分叉/飘带当作标志，未建立足够清晰的品牌识别。V1 文件保留，不用于安装替换。

本轮先研究官方规范、品牌/商店案例，再重新生成原创符号。研究见 `RESEARCH.md`；以下方案与视觉判断是本项目的设计提案，不是他牌创作意图的事实。

## 新设计

### A · 归潮（优先继续打磨的方向）

纸船上部保留折纸识别，下部由一条潮线承托。纸船来自本 App 已接受的封存载体，潮线表达漂泊；以海水蓝、浅沙色连接海面开屏和退潮首页。轮廓在本次 48px 预览仍能分出船体和水线。

优势：不仅说明“海洋”，还说明“有东西交给时间”；比地貌缩略图更贴近记忆产品。

待评审：纸船是否有航海工具的误读；船尖、船身负空间与潮线的关系仍可精修。图片生成结果包含少量折面色差，不是正式矢量字标。

### B · 潮汐的未（名称识别对照）

使用单个「未」字作为标志，保留上短下长两横、中央主干和左右伸展结构；轻微曲线连接洋流主题。

优势：本次 48px 预览字形清楚，名称关联强。

待评审：当前结果更像柔化的字标，潮汐/洋流语义弱于 A；不能仅因可读就宣称设计已经通过。

## 开屏衔接边界

- 图标是主题符号，不再使用海面或潮滩照片作图标。
- 蓝色来自现有海水色系，浅色来自潮滩沙色；图标不必复制每一时段的照片与光照。
- 保留已认可的图片首帧、海面退潮到首页路径；不插入额外 Logo 品牌页、不为图标更换背景图。
- 本轮只设计候选和视觉对照，没有实现图标到海面 morph，没有改变 Flyme 启动动画或系统 SplashScreen。
- 若选择 A，优先通过前景纸船符号与蓝色背景的共同视觉建立衔接，不把 48px 图标本身强行放大成全屏照片。

## 交付与验证

- `a-returning-tide.png`、`b-unfinished-current.png`：独立生成的 1254×1254、非透明完整方图。
- `review.png`：1280×1060 对比图，包含 48/64 CSS-like pixel 的等比预览、96px 圆形裁切及日光/斜阳/夜色海面与首页参考。
- `render-review.ps1`：只做评审排版、缩放和裁切；不修改候选 PNG 原图。
- 图像通过内置 imagegen 模式生成，没有使用 CLI/API 回退，也没有用他牌 Logo 作生成参考。
- 当前属于待用户评审的设计稿，不是 Android adaptive foreground/background/monochrome 图层；未构建、安装、替换启动器，也未更新待办状态。
- 已视觉检查原图与对比图，核对本地文件尺寸。圆角/圆形预览不替代 Launcher mask / 真机测试。
- 浏览器打开官方案例页面发生超时，未以截图检索成功作保证。研究报告应明确案例形态与创作意图的证据边界。

## 提示词与生成出处

### A · 归潮

```text
Use case: logo-brand.
Asset type: one original Android launcher icon concept for the Chinese personal-memory app Unfinished.
Primary request: a sophisticated brand symbol for a memory entrusted to an ocean current and later returning to shore. The app has calm realistic overhead sea photography, pale tidal-flat land and clear liquid-glass controls; its actual memory-sealing animation is a paper boat carried by currents.
Composition: ONE compact, optically centered ivory symbol on a full-bleed square sea-blue background. The symbol is a very simplified folded paper boat whose lower hull flows into a single graceful returning-tide curve. Treat boat and tide as ONE designed emblem, not separate illustration stickers: crisp origami upper silhouette, a restrained flowing lower contour. Two broad fold planes at most, one clear blue negative-space diagonal fold. The returning tide is a short broad asymmetric sweep under the hull, open-ended, visually integrated with it. Very few shapes, no tiny lines.
Style: premium modern app identity, clean precise graphic geometry with soft natural curvature, quiet and contemplative. Nearly flat ivory foreground; only a whisper of tonal separation between two fold planes. No rendered sea or environment.
Palette: muted luminous sea teal around #5897A5 or #568F9C; warm pale sand ivory #F3EDDF; one slightly darker sea-blue cut. Calm and bright, not neon cyan or murky gray. Background can have a very subtle broad tonal drift, no texture.
Constraints: center motif inside central 60% canvas, generous margin, clear bold silhouette at 48px. Full-bleed square, no rounded corners baked into artwork, no outer shadow. This is an icon not a wallpaper, sticker, render, scene or presentation board.
Avoid: words, letters, title, border, realism, aerial sand island, tidal-tree branches, waves texture, gloss, translucent glass orb, metallic bevel, multiple parallel ribbons, whirlpool, sun or moon, droplets, extra objects, existing brand logos.
```

```text
Generated images are saved to <local-generated-images> as <local-generated-images>\exec-cb6f353e-31b2-41af-a955-d67cdc5edd72.png by default.
```

### B · 潮汐的未

```text
Use case: logo-brand.
Asset type: one original Android launcher icon for the Chinese personal memory app 未完成 (Unfinished).
Primary request: a custom designed single Chinese glyph "未" transformed gently into a tidal/ocean-current symbol while remaining legible as 未. This is a brand monogram, not a typeset title. Design the recognizable five-stroke skeleton of 未: the upper horizontal bar is shorter than the middle horizontal bar; a central stem; two lower outward arms. Keep the proper character structure so it is NOT 末, 木, 朱, a yen symbol or an upright botanical tree.
Design: use thick smooth ivory strokes with softly rounded but crisply drawn contours and spacious gaps. Both transverse strokes have restrained shallow tidal curvature; the central lower stem bends subtly like a current; the lower arms sweep outward naturally. Preserve legibility and balance before adding the idea of flowing water. No separate waves, no additional pictograms. ONE glyph only, clearly centered.
Style: premium original graphic app identity, modern soft humanist lettering, quiet organic elegance, nearly flat. No calligraphy brush texture, fancy serifs, wobbly line or ornamental flourishes. A recognizable memorable silhouette at 48px.
Palette: full-bleed square muted sea-blue background around #5897A5 / #568F9C, clean warm sand-ivory glyph #F3EDDF. These correspond to the app's realistic sea opening and exposed sandy tidal-flat home. Background may have barely perceptible broad shading, no photo texture or glass.
Constraints: centered motif width about55% canvas, generous safe margin for circular masks, solid thick strokes, ample breathing room. No baked corner mask, no border or drop shadow.
Text verbatim: "未", exactly once, as the central custom logo glyph; no other text.
Avoid: photo, river landscape, realistic tidal tree, beach miniature, thin branching veins, floating decorative ribbons, closed vortex or infinity sign, coins, gold, metallic bevel, glossy 3D, app mockups, presentation boards, labels, existing trademarks.
```

```text
Generated images are saved to <local-generated-images> as <local-generated-images>\exec-6c226454-56d0-402a-823e-41936a773b21.png by default.
```


