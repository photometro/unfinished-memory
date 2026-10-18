# 未完成图标候选 V3

日期：2026-10-10。状态：待用户评审，未采用、未安装。

## 本轮边界

用户否定前两轮的图标，批准继续制作新候选。上一轮的纸船与字标不再作为本轮方向。保留所有旧文件；不改 App 图标资源、开屏、首页、折纸船洋流动效、待办清单和业务代码。

使用 imagegen 技能，以内置工具生成；没有 CLI/API 回退。新候选避免强行叠加文字、船、树、日月与标语，转向图标画面本身的水流和色彩关系。并未重新声称前两轮调研的图形已经得到用户认同。

## 筛选与判断

- 先试两张符号稿：潮线仍像狭长飘带；流隙的两块形状过于圆钝。它们保留在 drafts 下作为未入选试稿。这是本轮设计自审，不是用户已评审。
- A · 水光：光线和水面层次接近现有开屏，画面连续性较强。实际生成结果保留了比提示词预期更多的水纹和光线，偏氛围图；品牌符号辨识度较弱，不能因为水色漂亮就当作正式标志。
- B · 潮岸：两大色区和一条曲线，小尺寸结构更明确。海水与浅沙色对应开屏和首页。当前外形仍可能显得通用，独特性需要用户评审，不宣称已定版。

## 预览与验证

- 原图分别独立生成，完整方图、无预制圆角。
- review.png 用 System.Drawing 做评审排版、等比缩放和遮罩；没有编辑或调色候选原图。
- 预览包含 300px 圆角方图、48px/64px 小图与 96px 圆形裁切，并对照现有日光开屏/首页资产。
- 这些不是系统 Launcher 的实际测量尺寸、正式 adaptive foreground/background/monochrome 图层，也不是启动变形或真机截图。
- 后续仅在用户选定方向后制作正式图层，当前不替换任何已安装图标，不增加启动品牌页。

## 文件

- a-water-light.png：A · 水光。
- b-quiet-shore.png：B · 潮岸。
- review.png：对比预览。
- render-review.ps1：评审图排版脚本。
- drafts/：两张未入选符号试稿。

## 提示词与来源

### 潮线（试稿，未入选）

生成来源：`<local-generated-images>\exec-af9d89fc-3f4e-4fee-a3c1-61dd87c88058.png`。

```text
Use case: logo-brand.
Asset type: one original mobile launcher icon concept for the personal-memory app Unfinished, not a contact sheet.
Primary request: an elegant quiet ocean-current identity. The app opens on luminous turquoise overhead seawater, then reveals warm ivory tidal flats. Translate that atmosphere into a disciplined memorable symbol, not a miniature landscape.
Design: ONE ivory flowing tidal stroke on a clear sea-blue field. A broad relaxed horizontal current with one shallow rise and one shallow fall; its silhouette is gently asymmetric, with a fuller curved left section and a softly tapering right end. It should feel like water sliding quietly past, not a breaking surf wave. A single generous open blue cut within the broad stroke gives it air and depth without adding more strokes. The entire mark is compact and optically balanced, roughly 56% of square width and 30% of square height, placed at the exact optical center with spacious margins. Natural fluid contour, excellent positive and negative-space balance, drawn with confident smooth edges.
Style: refined graphic app identity, near-flat 2D, soft organic geometry but not rubbery lettering, crisp vector-friendly silhouette. Calm, clear, airy. A beautiful restrained form first, no illustrative storytelling.
Palette: full-bleed square luminous sea teal-blue around #459BAD, with minimal broad tonal variation; pale warm ivory #F4EFE4 foreground. Brighter and clearer than a dusty dark teal, not neon, not cobalt.
Constraints: single icon filling the square canvas; no baked rounded corner mask, border, framing, external shadow, labels, letters, text or watermark. Must read cleanly at 48px and inside a circular mask. No paper boat, no Chinese character, no tree, no envelope, no sun or moon, no circles or tiny dots.
Avoid: pointed claw/wing silhouettes, multiple parallel feather ribbons, surf curl, whirlpool, infinity sign, swan or bird, knotted paths, ornamental flourishes, company swoosh with a sharp spear tip, glossy 3D, glass orb, beveled metal, gradients inside the symbol, sea photography or sand texture, presentation mockup, existing brand logos.
```

### 流隙（试稿，未入选）

生成来源：`<local-generated-images>\exec-644c587a-ccd8-42b2-a5f8-4391db7ba360.png`。

```text
Use case: logo-brand.
Asset type: one original square app launcher icon design for Unfinished, a quiet natural personal-memory app.
Primary request: a graceful ocean current expressed through negative space. Create an original polished graphic icon, not a painting or decorative sculpture.
Composition: two clean broad warm-ivory organic shapes face each other and together define ONE open, gently S-curving sea-blue channel between them. The blue channel runs diagonally from lower left toward upper right with one unhurried bend, never coiling or intersecting. The ivory shapes are short, soft and compact, offset slightly rather than mirrored; their outer silhouettes create a balanced central emblem, about 58% of canvas width and 54% height. Both ivory shapes float within ample surrounding blue space; none touches the canvas edge. All curves have relaxed rounded ends and generously open gaps. The central channel is broad enough to remain obvious at 48px. The result should read as flowing water and breathing space, not two animals, a leaf, a yin-yang symbol, letter S, flame or a recycling mark.
Style: beautifully considered nearly flat 2D graphic identity, precise confident organic outlines, luminous calm natural mood. One simple figure-ground relationship, no extra line work. Ivory foreground in a single flat tone. No small details, no branches and no geometric ornament.
Palette: clear airy sea-blue teal background around #459BAD / #56A8B5; warm sand ivory #F4EFE4. Tiny broad background tonal drift is acceptable; no multicolor gradients or murky gray. Harmonizes with an overhead turquoise sea opening and pale sandy tidal-tree home without reproducing those photographs.
Constraints: ONE complete full-bleed square icon asset, optical center, large safe margins for circle masks. No text, typography, labels, border, baked rounded corners, external shadow, phone mockup or watermark. No photographic scenery, no paper boat, no Chinese character, no tree, no sun, no moon, no droplets, no jewels.
Avoid: glossy 3D, inflated glass, ribbons, feather shape, sharp spear tips, tangled double loops, closed vortex, infinity symbol, tiny contour lines, stock corporate swoosh, existing trademarks.
```

### 水光

生成来源：`<local-generated-images>\exec-e1d08993-4d20-4c35-9da9-ba1a2f06bc4b.png`。

```text
Use case: stylized-concept.
Asset type: ONE refined square launcher icon artwork for Unfinished, a natural contemplative memory app.
Primary request: compress the feeling of clear overhead ocean water into a beautifully simple abstract app icon. The water itself is the identity, not a white logo pasted onto a background.
Composition: a luminous sea-blue square with TWO broad gently flowing water surfaces, separated by one elegant shallow diagonal current boundary. A pale aqua translucent surface flows from the lower left across the central canvas and swells gently toward the upper right; underneath is slightly deeper clear blue water. The broad light and dark areas make one relaxed, visually balanced flowing gesture, not long thin ribbons. Keep all curves unhurried and rounded, no breaking wave, no curling spiral. One very restrained soft pale water-light edge follows part of the current boundary; broad tonal transitions, not a glitter line. The overall square is composed as an icon with a clear dominant curved boundary and substantial uncluttered regions, rather than a cropped wallpaper. No separate centered logo.
Style and materials: sophisticated soft digital illustration of water, semi-abstract, luminous translucent aquatic layers with subtle depth and light diffusion, nearly smooth. Not a 3D sculpture, not inflated plastic, not shiny bevel. Contemporary, restrained, quiet and sensuous. No literal photographic wavelets or foam. Good shape readability at 48px.
Palette: clear turquoise and sea-blue around #459BAD, #70C0C8, #B3DEE0, a restrained pale ivory-blue light edge. Avoid purple, neon, dull gray or deep dark blue. Harmonizes with realistic clear aqua ocean opening and warm ivory tidal-flat home.
Constraints: full-bleed SQUARE source image, no rounded corners baked in, no border, no external drop shadow, no text or label, no watermark, no mockup, no trees, no land, no sun, no moon, no paper boat, no glyph, no orb, no drops, no objects.
Avoid: multiple feather ribbons, company swoosh, knotted currents, infinity symbol, ring or vortex, busy microtexture, storm surf, triangular sharp tips, segmented neon waves, mermaid/fish shapes, a central white pictogram, existing app icons.
```

### 潮岸

生成来源：`<local-generated-images>\exec-85c7e072-c296-4130-a62d-93a73528ab5f.png`。

```text
Use case: logo-brand.
Asset type: ONE square mobile app icon artwork for Unfinished, quiet personal memories entrusted to time and ocean currents.
Primary request: a restrained organic coastline abstraction with luminous ocean color and warm sand, using two large color regions as the complete icon. It must feel beautiful and natural before carrying symbolism.
Design: full-bleed clear blue water occupies roughly the lower 55% of the square, pale warm ivory occupies the upper 45%. They meet along ONE broad asymmetric gently flowing shoreline: from upper-left-middle, the sea makes a single smooth shallow rounded inlet into the ivory region near center, then relaxes toward lower-right-middle. Make the silhouette composed and graceful, not a sharp or deep S zigzag. The interface is a wide, open, relaxed curve visible at 48px, with absolutely no branches, islands or tiny coastline details. Both colored regions continue naturally to canvas edges. This is a graphic icon, not an aerial landscape. A thin quiet pale-aqua transition zone along part of the boundary suggests shallow water; integrated natural colors, not a bright white outline.
Style: elegant contemporary nearly-flat abstract graphic, crisp flowing large shapes, subtle restrained color depth in the blue, ivory mostly flat. No symbol floating on a background. No logo sticker. Clean breathable image which blends with a realistic overhead sea opening and pale tidal-flat homepage. Soft natural contours without geometric circles or gummy blob shapes.
Palette: warm sand ivory #F2EDE2, luminous blue-teal water #459BAD and #6BB6C0, a narrow pale aqua shallows band around #A6D4D5. Very gentle natural tonal transition only, no sunset colors, no neon.
Constraints: one complete full-bleed square artwork, no baked corner mask, frame or border. Strong simple figure-ground composition inside both circle and rounded-square preview. No text, label, typography, watermark, phone mockup, birds, sun, moon, boat, tree, letter, cloud or separate motifs.
Avoid: miniature landscape, photographic waves, sand grain or foam texture, branching veins, sharp cape, isolated central island, decorative ribbons, whirlpool, giant shiny glass wave, 3D raised edges, thick drop shadow, stock corporate emblem, existing brand logos.
```


