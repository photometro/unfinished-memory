# 未完成 · 潮汐洋流图标候选 V1

日期：2026-10-10。

## 范围与状态

新图标候选已制作，等待用户选择与进一步打磨。尚未替换 Android 启动器资源、未安装到真机、未改变已认可的海面开屏与退潮动画，也未更改待办状态。

使用内置 imagegen 生成/编辑模式；每个候选为独立 PNG，不是一张拼接展示板。最终原图均为 1254 × 1254、不透明、完整方形画布。圆角和圆形由预览页面裁切；这不是正式 Adaptive Icon 图层交付。

## 设计判断

- A「潮沟入海」：图形化潮沟与主干，作为后续品牌化的优先方向；仍需评审分叉形态是否过于像植物，以及小尺寸的粗细。
- B「洋流回湾」：三条同向水带与浅沙洲，流动感更强，但潮汐树识别弱于 A；需要评审是否像羽毛/飘带而非洋流。
- C「自然潮滩」：更贴近首页的自然材质；保留为对照，48px 下小分叉与水纹辨识有限，暂不作为小尺寸最终定稿。

图标与开屏的联系是同一海水蓝、浅沙色和地貌主题：图标浓缩环境，进入 App 后海面铺开，退潮显露潮沟。不是重新插入居中 Logo 系统启动页；此次未实现图标形态变换或修改设备图标打开动画。

## 文件

- `a-tidal-channel.png`：A 第二轮简化结果。
- `b-current-cove.png`：B 第二轮简化结果。
- `c-natural-shore.png`：C 第一轮自然材质结果。
- `index.html`：三方案对比、48/64/96 CSS px 与圆形/圆角/方形裁切、当前开屏/首页参考。

## 参考

生成前已查看当前 App 的 `android/app/src/main/res/drawable-nodpi/sea_daylight.png` 和 `shore_daylight.png`。它们是颜色/材质参考，不是待改图片。旧三卡图标保留不变。

## 检查记录

- 已逐一查看生成图，并对 A、B 的纹理与分叉进行第二轮简化。
- 已核对三张最终 PNG 的尺寸、文件可读性，以及预览页全部本地资源引用均存在。
- 浏览器拒绝自动访问 `file://` 本地对比页，未绕过限制，因此尚未完成浏览器截图、裁切按钮交互与小尺寸视觉 QA；用户可以从已保存的文件进行预览。小尺寸可用性仍待人工评审，不能据此宣称通过真机图标验收。

## 原始提示词与生成出处

### A · 潮沟入海

第一轮（使用当前海面、潮滩两张参考图）：

```text
Create ONE finished square Android launcher icon concept for the personal memory app Unfinished, whose visual identity is sea surface -> ebb tide -> exposed dendritic tidal channels. Use the two supplied images ONLY as color/material/style reference: first is the app's actual opening sea surface, second its actual tidal-flat homepage. This is a NEW icon, not a crop of either photo. Full-bleed square image, no phone, no presentation board, no text, letters, watermark, enclosing border or baked rounded-corner tile. Keep the meaningful central motif inside a generous centered safe region so circle and rounded-square cropping both work; visually centered, balanced negative space, recognizable at 48px. Calm turquoise/blue-grey water and warm pale ivory sand, refined natural visual language, clean soft forms and enough tonal contrast, not washed out. No paper boat, card stack, sun, moon, generic wave emoji or orange dot. Direction A: graphic-natural tidal delta emblem. Against a calm muted turquoise sea field, one centered compact warm ivory tidal-flat landform. A prominent meandering blue S-shaped channel opens toward the lower right into the surrounding sea and divides inland into three bold organically tapered branching channels toward the upper half. Only two levels of branching, very few well-spaced branches. The shape must read as tidal channels cut into sand seen vertically from above, NOT a botanical tree standing upright and NOT a leaf. Channel widths naturally taper inland; the main outlet is broad. Rounded erosion contours, nearly flat restrained shallow relief at the bank edges, no heavy cast shadow, no microscopic sand detail. An elegant compact land/water silhouette, approximate 65 percent central motif width. The blue negative-space trunk carries a subtle continuous curved current sweep into the ocean, without arrows or extra lines. Clear, quiet, iconic.
```

生成出处：

```text
Generated images are saved to <local-generated-images> as <local-generated-images>\exec-8d008fb9-6186-4be1-86a6-a524e6b4f1a6.png by default.
```

第二轮（编辑第一轮生成图，减少细节）：

```text
Redesign this icon as a genuinely simplified graphic launcher icon, not a photograph. Keep ONLY the idea of one ivory tidal-flat silhouette with a blue S-shaped main tidal channel opening to the bottom right and dividing inland. Replace ALL photographed water ripples, caustics, sandy texture, realistic shadows and microtributaries with perfectly smooth flat or gently gradient fills. Use just 3 main branches, each with at most one short fork, thick easily readable cuts. Keep pale sand as one compact optically centered island landform; clean curved erosion contours, no sharp angular points, no illustrative standing tree or leaf. Sea background is a single quiet muted blue-turquoise solid field, roughly #68AAB7, with extremely subtle large-scale shading only. Sand approximately #F0E8D8, darker blue channels approximately #3C899F. Entire composition should look like premium crisp two-tone minimalist map geometry with very subtle satin bevel only if needed, no photo textures whatsoever. Full-bleed square, central main motif around 62% width, strong icon readability at48px and circular mask. No text, no border, no baked rounded tile. Do not add new objects. Preserve sense of one continuous tidal inlet leading into an ocean current.
```

最终出处：

```text
Generated images are saved to <local-generated-images> as <local-generated-images>\exec-e209d540-7c04-44d1-96d0-8592c09e6dc7.png by default.
```

### B · 洋流回湾

第一轮（使用当前海面、潮滩两张参考图）：

```text
Create ONE finished square Android launcher icon concept for the personal memory app Unfinished, whose visual identity is sea surface -> ebb tide -> exposed dendritic tidal channels. Use the two supplied images ONLY as color/material/style reference: first is the app's actual opening sea surface, second its actual tidal-flat homepage. This is a NEW icon, not a crop of either photo. Full-bleed square image, no phone, no presentation board, no text, letters, watermark, enclosing border or baked rounded-corner tile. Keep the meaningful central motif inside a generous centered safe region so circle and rounded-square cropping both work; visually centered, balanced negative space, recognizable at 48px. Calm turquoise/blue-grey water and warm pale ivory sand, refined natural visual language, clean soft forms and enough tonal contrast, not washed out. No paper boat, card stack, sun, moon, generic wave emoji or orange dot. Direction B: sculptural ocean-current emblem, clearly different from a tree logo. A centered open sweeping C-like bend made of three broad smooth laminar water bands curves diagonally from upper left toward lower right around ONE small warm ivory crescent sandbank inside the bend. The bands are turquoise, softly luminous pale blue, and deeper teal; distinct enough to read at small size. All stream bands travel in the same direction, remain parallel, never cross, knot, form an infinity symbol, or close into a whirlpool. The current bend is open toward the lower right, suggesting water continuing beyond the icon. One or two wide tapered blue tidal inlets subtly cut the sandbank, linking the river-current and tidal-flat identity. Compact optically centered composition, fluid matte-to-satin material, barely raised forms, no glass orb and no chrome. More ocean/current than land, bold shapes, clean limited geometry.
```

生成出处：

```text
Generated images are saved to <local-generated-images> as <local-generated-images>\exec-4c59c2c8-bfc4-40d4-8aee-f99f77184481.png by default.
```

第二轮（编辑第一轮生成图，减少细节）：

```text
Simplify this ocean-current image into a clean sculpted Android launcher icon emblem with bold large shapes, NOT a miniature photo. Keep the open sweeping diagonal current around one pale ivory crescent sandbank, but completely remove all photographic ripples, caustics, foam, sand texture, complex tiny channels and horizon. A smooth medium teal full-bleed square background approximately #5798A8. Three broad smooth gently luminous blue water ribbons with generous separation curve together in ONE same flow direction around a small central ivory crescent sandbank. Ribbons stay parallel and open, no crossing, knot, infinity loop, closed vortex or circling arrow. The entire ribbon motif occupies around62% canvas and is balanced at true optical center, with generous plain blue margin. One simple tapered inlet cut in the crescent suggests the tidal shore. High-end graphic-sculptural rendering, smooth matte/satin surfaces, very restrained depth, large readable color blocks, cream and cool sea blue palette, elegant negative space. No white glowing outline, no heavy shadows, no glossy chrome, no letters, no logo text, no frame, no rounded tile baked in. This should be an icon symbol of flowing water and revealed shoreline, not actual scenery.
```

最终出处：

```text
Generated images are saved to <local-generated-images> as <local-generated-images>\exec-74e1b611-b003-46a3-9660-8aaa6767ff68.png by default.
```

### C · 自然潮滩

第一轮（使用当前海面、潮滩两张参考图）：

```text
Create ONE finished square Android launcher icon concept for the personal memory app Unfinished, whose visual identity is sea surface -> ebb tide -> exposed dendritic tidal channels. Use the two supplied images ONLY as color/material/style reference: first is the app's actual opening sea surface, second its actual tidal-flat homepage. This is a NEW icon, not a crop of either photo. Full-bleed square image, no phone, no presentation board, no text, letters, watermark, enclosing border or baked rounded-corner tile. Keep the meaningful central motif inside a generous centered safe region so circle and rounded-square cropping both work; visually centered, balanced negative space, recognizable at 48px. Calm turquoise/blue-grey water and warm pale ivory sand, refined natural visual language, clean soft forms and enough tonal contrast, not washed out. No paper boat, card stack, sun, moon, generic wave emoji or orange dot. Direction C: an art-directed realistic miniature tidal-flat scene seen directly overhead, compact and icon-readable, more natural than graphic A. A single large turquoise branching tidal inlet curves through a compact ivory sand peninsula, entering from the lower right and forking toward upper left and upper middle. Show only 5-7 thick main branches and a few visible minor branches, not hundreds of hairline tributaries. Pale fine-grained sediment softly sculpted by receding water, softly lit daylight, thin damp bank edges, quiet smooth water without whitecaps, little shimmer. No horizon. The peninsula and thick blue dendritic channels form one centered distinctive silhouette, with water framing all edges. Preserve a broad simple center and generous uncluttered surrounding blue. Premium calm photographic-material icon, subtle believable depth, crisp enough at small size, not a satellite screenshot or busy map.
```

生成出处：

```text
Generated images are saved to <local-generated-images> as <local-generated-images>\exec-286818dd-aa78-49cd-a604-f8b8efb09298.png by default.
```


