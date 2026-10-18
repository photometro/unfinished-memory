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
