# 未完成 · 图标候选预览

这是一次性的视觉选择原型，不会修改 Android 工程中的当前图标。

在 PowerShell 中运行：

```powershell
Set-Location '<project-root>\design\icon-options'
PowerShell -ExecutionPolicy Bypass -File .\start.ps1
```

候选文件：

- `icon-card-gap.svg`：未封口的记忆卡片
- `icon-folded-page.svg`：未写完的纸页
- `icon-open-loop.svg`：未完成的记忆回环
- `icon-three-cards.svg`：首页三张记忆卡片（首页配色与交错关系）
- `icon-three-cards-balanced.svg`：首页三张记忆卡片（D 紧凑居中优化版）
- `icon-three-cards-horizontal.svg`：首页三张记忆卡片（左右重叠版）

当前定版：`icon-three-cards-balanced.svg`（D2 三张纯色卡片，上下重叠、左右分离增强）。
