# B · 潮岸 V6：中间色彩候选

日期：2026-10-10。状态：未定版，仅视觉候选。

## 用户要求与更正

- 新增清亮版与质朴版之间的一版；前者过于鲜亮，后者主观上偏暗、偏灰。
- 延续沙滩与海浪，不恢复潮沟。
- 用户在生成期间明确要求不要参考此前展示的潮滩照片。

因此，带该实拍输入的第一轮结果不采用、不复制到项目，不作为后续编辑基础。最终重新生成，输入仅为 V5 清亮版和 V5 质朴版；提示词明确排除其它照片与之前的候选。

## 素材核对的限制

本次曾检索真实沙滩/海浪照片页面，但远程位图未成功显示；浏览器核对被权限检查拒绝。没有下载照片来绕过限制，也未再访问该参考。最终图不属于实拍采样调色，不声称其色值准确还原某个真实地点。

“自然”是本轮色彩设计方向：减少电蓝感，保持适量蓝绿彩度和浅米沙色，而不是客观认证的真实颜色。

## 交付与方法

目录：`<project-root>\design\icon-options\tidal-20261010-v3`。

- `b-quiet-shore-v6-natural.png`：新增中间色候选，1254×1254。
- `review-b-shore-v6.png`：三版对照，左清亮、中新稿、右质朴，1320×740。
- `render-shore-review-v6.ps1`：仅等比缩放、遮罩裁切预览，不修改原图像素。

采用内置 imagegen 编辑模式，`transparent_background=false`，非 CLI/API。
不修改 App 图标、开屏、原型、待办、安装或签名；保留旧稿。

## 观察与验证

- 新稿没有潮沟或其它新符号，海岸位置和主要浪痕基本延续 V5 清亮版。
- 水色由高彩度蓝色转向适中彩度蓝绿；沙色减弱黄色倾向。质朴版中的整体灰感没有直接覆盖到新稿。
- 已查看完整图和 48、64 px 圆角、96 px 圆形预览。只是候选外观检查，不代表正式 adaptive icon 安全区或真机通过。
- 生成式编辑不是像素级只改色值，局部水纹仍有轻微变化。

固定水面区域（宽 15%–85%、高 65%–95%，每 16 px 采样）平均 HSL 饱和度：清亮版约 0.984，新稿约 0.672，质朴版约 0.362。用于确认这一区域的彩度处于两版之间，不证明主观明暗合适，也不证明颜色真实。是否达到用户希望的中间状态，仍需用户审核。

旧两版 SHA256 与上一轮记录一致，未修改：

- 清亮：`64408142B610289E49FBF7AEF633BDF35B693CA3E7E4760EBFF535D79953E628`。
- 质朴：`DDD8DEECE61309E29BE31681A045AFD1D047C262D7A3408CBD5F8A84284BE2FE`。
- 新稿：`8F4A1597C34868AFF7E9867AC17E9E16203A8A7BEBAB309F444637B9EA382728`。

## 最终完整提示词

输入顺序：
1. `b-quiet-shore-v5-bright.png`，编辑目标。
2. `b-quiet-shore-v5-muted.png`，仅色彩比较。

```text
Use case: precise-object-edit.
Asset type: one new square Android app icon colorway for Unfinished.
Inputs: Image 1 is the EDIT TARGET: the bright beach/sea icon. Image 2 is ONLY the muted palette comparison. Neither is a photo. Do not use or derive anything from any other previously provided photo or previous generated candidate in the conversation.

Primary request: recolor Image 1 to a balanced, natural daylight beach palette between the two icons. The first is too electric blue and yellow-bright, the second is too grey, dull and dark. Aim for ordinary, believable clear coastal water and light sand rather than neon travel-advertisement colors or faded grey vintage colors. Do NOT merely darken the first icon.

Color direction: clear medium-chroma sea blue with a gentle green undertone, noticeably less saturated than Image 1 and noticeably more fresh and luminous than Image 2. Water tonal guides: main water #3F9BAE, slightly deeper areas #36879E, shallows #95C4CB. These are approximate tonal anchors, not uniform fills. Retain the sense of translucent water and natural subtle depth variation. No electric blue, fluorescent cyan, teal glow or dark navy. Sand: light neutral warm beige/ivory around #EDE5D7, less yellow than Image 1 but brighter and fresher than Image 2. Foam: soft near-white, not overexposed brilliant white. Keep a gentle daylight range, no fog or overall grey veil. Adjust the water hue/chroma and the sand yellow cast separately, not a uniform desaturation of the entire picture.

Strict invariants: preserve Image 1's exact shoreline position and open coastal arc, sea/sand proportion, top-down view, existing gentle foam and water-wash distribution, style, framing and texture. Color and tone adjustment ONLY. No new geometry, stronger waves, tidal creek, branching network, river, tree, boats, shells, people, vegetation, text, logo, border, mockup frame, watermark, paper grain, golden reflection or sunset. Output one full-bleed square artwork, no comparison sheet.
```

最终内置生成源：`<local-generated-images>\exec-ace7c310-13bc-43cc-b364-3ecf413f80fd.png`。

