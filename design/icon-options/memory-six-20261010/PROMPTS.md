# 图标生成提示词

方式：内置 imagegen；未使用 API/CLI 回退。A 为上一版保留原稿；B/C 是物品图形，D/E/F 是对应物品的解构探索。所有输出为待用户选择的预览，不代表已替换 App 图标。

## A 原始方案

# 未完成图标探索：翻开的记忆

状态：待用户审美审核的单方案预览，未替换 App 图标，未构建或安装。

## 产品 → 物品 → 风格 → 图标

1. 产品：保存文字、照片与生活片段；封存并沉寂；在未来以线索重新接近过去。不是任务清单、普通相册或社交应用。
2. 代指物品：一张正被重新翻开的照片。照片代指被保留的生活片段，翻角代指重新打开；不要求用户先理解纸船或潮汐算法。
3. 风格：克制的纸面解构。保留易认的照片轮廓，用一个翻角拆分局部形态；以海蓝、象牙纸色和沙色连接现有 UI。
4. 图标：单主体、宽边框、简化海岸线、无文字。大图不追求复杂的自然景观，缩小后优先保留物品识别。

局限：照片会带来相册联想，不能仅凭一张图标让首次见到它的人准确说出产品名称或全部功能。名称标签及持续使用仍承担品牌识别；本方案的目标是先建立“保存与重新打开记忆”的视觉线索。

## 生成与输出

使用内置 imagegen 工具，未使用 API/CLI 回退。原始生成图保存在 Codex generated_images；此目录保留 1024 原图版本、48/64/96 像素缩略图及预览。

缩放与圆形/圆角裁切仅用于检查桌面尺寸，不对生成图进行艺术性重绘。尚非 Android Adaptive Icon 前景/背景分层交付。

## 完整生成提示词

Use case: logo-brand. Asset: one polished square launcher-icon design for Unfinished, a private app that preserves personal memories and lets users rediscover them later.

Core object: ONE instant photograph being gently opened at a folded corner. The photograph is the entire central emblem: a recognizable warm ivory paper frame with a single lifted corner and a simple image area. It represents a saved moment being revisited. Choose an exceptionally clean, memorable silhouette that remains recognizable at 48px.

Style: refined minimal graphic design with restrained paper deconstruction. Construct the object from a few crisp, generous colour planes. A deliberate small separation at the lifted corner reveals the fold; otherwise the photo remains complete and easy to read. Almost flat, with just a subtle tonal change between the fold faces. No paper grain or rendered realism.

Composition: one balanced compact photo mark, nearly square, very slightly tilted, centered with generous safe margins. Fill about 65 percent of the square. Small corner radii on the photo, wide readable paper edges, one broad image region. The lower white margin is modest, not a huge blank strip.

Inside the photograph: reduce the app's tidal coast to two large calm colour planes, warm sand and muted deep-blue water separated by one softly irregular sweeping shoreline. No photographic wave detail. The shoreline and the paper fold form a coherent visual rhythm, suggesting that the preserved memory is being opened again. The folded corner must clearly belong to the photograph, not appear as a second object.

Palette: full square background in muted mineral sea blue; warm ivory paper; pale sand; slightly darker teal-blue water. Clear shape contrast and quiet warmth. Premium, calm, distinctive and practical as a launcher icon.

Constraints: no words, no letters, no numbers, no title, no boat, no shell, no separate decorative waves, no extra props, no mountain-and-sun stock photo symbol, no multiple cards, no elaborate shadows, no gradients, no grain, no glossy glass, no landscape illustration, no mockup, no border or rounded-square mask. Output the single final square artwork only.



## B 记忆册

Use case: logo-brand.
Asset type: one finished square Android launcher icon for Unfinished, a private personal-memory app for leaving moments now and rediscovering them in the future.
Product-to-object concept: an OPEN MEMORY JOURNAL. A small journal preserves lived moments; an open spread represents revisiting what was kept. The journal, not a landscape, must be instantly recognizable.
Composition: one large compact open notebook viewed almost from above, slightly oblique but not isometric. Two broad warm ivory pages, an unmistakable central gutter, a muted deep-sea-blue hardcover visible behind their edges. Gently asymmetric page turns give life to the silhouette. Center the emblem with generous mask-safe margins, filling about 68 percent of the square; keep it balanced, nearly square overall rather than long or thin.
Style: refined minimal paper sculpture, restrained deconstruction of the page planes. Clean bold shapes, soft small corner radii, just enough fold shading to explain the volume. No detailed paper texture. Calm, confident, useful as a 48px app icon.
Meaningful detail: one broad muted blue tidal curve crosses the lower part of the open pages as though the remembered coast is preserved within the book; the upper paper areas remain calm and spacious. It should be an integrated simple blue-and-sand colour cutout on the pages, NOT separate decorative waves surrounding the book. Use only one clear shore curve, no tiny doodles.
Backdrop and palette: full square warm pale sand background, warm ivory paper, slate-teal blue cover, subdued blue water. A slightly darker spine creates a clean readable silhouette. Quiet natural hues, not bright saturated beach colours. Object contrasts clearly against the background.
Constraints: no words, letters, numbers, lines of text, pens, locks, checkmarks, clock, calendar, multiple separate objects, boat, photo frame, sun or mountains. No overly glossy rendering, no dramatic cast shadow, no busy waves, no floating decorations, no mockup, no rounded-square outer mask. Deliver only one square icon artwork.

## C 漂流瓶

Use case: logo-brand.
Asset type: one finished square Android launcher icon for Unfinished, a private app that preserves memories, lets them drift through time and allows a future encounter.
Product-to-object concept: a MESSAGE-IN-A-BOTTLE. A visible preserved paper fragment inside one closed bottle expresses leaving a moment safely; the current passing the bottle expresses a future rediscovery. It must read as a keepsake drifting bottle, never as medicine or a drink.
Composition: a single stout rounded glass bottle, almost-square body with short recognisable shoulders, short neck and one small warm sand cork. Large centered silhouette, tilted subtly about 8 degrees, with mask-safe margins and about 66 percent occupancy. View straight-on with gentle depth, not an overhead flat capsule. Inside the glass, ONE broad ivory loosely rolled paper sheet with a visibly curved upper edge, filling most of the body so the content stays legible when reduced.
Style: premium simplified translucent mineral glass, clean restrained surfaces, controlled refraction at the thick silhouette edge, a single soft edge highlight. Graphic and sculptural rather than photorealistic. No chrome, no iridescence, no tiny reflections. Warm human keepsake, not a sci-fi vial.
Integrated current: one broad calm slate-blue curved water band passes across the bottle's lower quarter, its continuity slightly refracted through the glass. This is one flowing band at the base, not a full ocean scene, not several surrounding curls or ripples. Keep the bottle and its paper as the first thing the eye notices.
Palette and backdrop: full square soft ivory-sand background; glass in muted clear blue-green with darker marine-blue edge; warm ivory paper and a sandy cork. Strong silhouette contrast, low saturation, natural warmth, coherent with a realistic tidal-coast app UI.
Constraints: no words, letters, numbers, labels, medical cross, stopper dropper, pill, perfume cap, envelope, boat, separate photograph, mountains, sun, elaborate shadows, paper grain, busy foam, multiple bottles, floating decorative elements, mockup, outer rounded-square mask. Deliver only the single square launcher-icon artwork.

## D 错位照片

Use case: logo-brand. Asset: a single square launcher icon for Unfinished, an app that preserves personal moments then lets people rediscover them later.
Meaning-to-object process: the surrogate is ONE KEEPSAKE PHOTOGRAPH being opened again. First retain a recognisable photograph frame, then deconstruct its planes to suggest a preserved moment still unfolding.
Visual style: sophisticated restrained deconstructivist cut-paper graphic, NOT realistic illustration. Few large matte solid colour shapes, crisp silhouettes, elegant negative space. No surface grain, no gradients, no shadows or rendered bevels.
Object and composition: one nearly square ivory instant-photo frame on a full muted marine-blue background, very gently tilted. At the upper-right corner, one large angular frame fragment has been cleanly separated and displaced by a tiny amount, with a narrow blue gap, as if the photo's corner has just unfolded. The four broad paper borders still form an obvious photograph. Use ONLY this one deliberate discontinuity; no shattering.
Inside the photograph: one sweeping pale-sand coastline and one large muted teal water plane. Let the shoreline continue into the displaced corner in a controlled misalignment, connecting the deconstruction to the memory metaphor. No photographic waves, no stock mountain or sun.
Balance: centered compact emblem, about 65 percent canvas occupancy; generous outer safe area; clear silhouette at 48px and under a circular mask. Preserve broad frame edges and only 3 or 4 major colour planes. Calm, human, distinct.
Constraints: no text, letters, numbers, symbols, random geometric decoration, arrows, rings, boat, bottle, book, multiple photographs, surrounding waves, ripped edges, jagged shards, texture, glossy 3D, mockup, rounded-square outer border. One square artwork only.

## E 潮页记忆册

Use case: logo-brand. Asset: a single square launcher icon for Unfinished, a private app for saving lived fragments and revisiting them later.
Meaning-to-object process: the surrogate is ONE OPEN MEMORY NOTEBOOK. The pages are kept moments; opening them means returning to the past. Deconstruction must respect the notebook's recognisable open-spread silhouette.
Visual style: refined deconstructivist editorial paper-cut, bold flat solid colours and slightly organic shapes. A compact emblem, not an illustration of a real book. No texture, photorealism, gradients, or cast shadow.
Object: two generous warm ivory paper panels form the left and right pages of an open notebook, with one clearly visible narrow deep-blue central gutter and an understated deep-teal cover silhouette underneath. One page is slightly offset upward; the other has a broad single turned-corner plane. The open book remains unmistakable. Use only two subtle constructive dislocations, no scattered page pieces.
Integrated tidal identity: a broad soft marine-blue flowing colour plane crosses the bottom portions of both pages; at the central gutter it is subtly offset, like two preserved fragments of the same remembered coast. One pale sand plane complements the blue. Keep the upper paper areas empty. The page edges and the current should share the same gentle sweep.
Backdrop: full-square soft pale sandy ivory, contrasted with deep muted blue cover, warm lighter paper and marine-blue water. Quiet natural colour, not pastel rainbow.
Composition: center a near-square, compact open-book emblem, about 67 percent canvas width; clean safe margins for a circular crop. Strong visual rhythm, broad edges legible at 48px.
Constraints: no words, letters, numbers, writing lines, pen, lock, calendar, checkmark, boat, bottle, photo frame, tiny details, separate decorative waves, fragmented confetti, irregular torn paper, realistic book rendering, shadows, mockup, rounded-square outer mask. Deliver the single square icon artwork only.

## F 洋流记忆瓶

Use case: logo-brand. Asset: one single square launcher icon for Unfinished, a private personal-memory app where a moment is kept, drifts through time and is encountered again.
Meaning-to-object process: the surrogate is ONE MESSAGE-IN-A-BOTTLE, a recognisable bottle with a saved paper note inside. Deconstruction expresses drifting fragments without losing the bottle silhouette.
Visual style: elegant restrained deconstructivist graphic, composed of just a few generous matte cutout colour planes. It is not a realistic glass rendering, not cubist clutter. Clean solid colours, organic flowing contour, no gradients, grain or shadows.
Object construction: one short-neck, broad-shouldered round-bodied blue-green bottle, softly leaning. A small sandy cork establishes that it is a sealed keepsake. The bottle body is constructed from TWO complementary deep-blue and lighter teal curved planes separated by a narrow curving ivory negative-space seam. The two planes almost touch and together form a clear whole bottle. One warm ivory paper panel is visibly preserved in the center, with one simple rolled edge. Do not split the neck or scatter the cork away.
Integrated current: the lower bottle contour opens briefly into one tapered flowing tail, suggesting a current carrying the keepsake. This is part of the same continuous bottle colour plane, not an extra surrounding wave or floating ornament. Maintain a stable compact silhouette and a readable saved note.
Palette: full-square warm ivory background, subdued sea-blue and desaturated aqua bottle planes, sandy cork, warm ivory paper. Strong outline contrast with broad readable colour areas.
Composition: large centered compact emblem about 64 percent occupancy, generous circular-mask safe margins, elegant asymmetric balance, legible at 48px.
Constraints: no text, numbers, symbols, label, medical cross, pills, dropper, perfume bottle, envelope, boat, book, separate photograph, multiple bottles, random shards, maze lines, splash, foam, glossy rendered glass, thin wire outlines, complicated highlights, mockup, rounded-square outer mask. Deliver the single square icon artwork only.
