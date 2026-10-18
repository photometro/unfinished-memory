# 未完成图标设计重新调研

日期：2026-10-11

## 结论

上一版暂不继续精修。合规的尺寸、可解释的符号与可编辑源文件，都不能替代对主体、比例、轮廓和气质的验收。

- [官方设计原则与上一轮复盘](design-principles.md)
- [GitHub 辅助工具核查](github-tools.md)

## 本项目建议

以下为基于资料与用户反馈的设计判断，不是官方规定：

1. 核心表达是“保存生活片段、未来重新遇见”，不要只表达海洋，也不要按“任务未完成”误画成待办。
2. 先确定有辨识度的主体轮廓，暂不限制为文字、纸船、照片、洋流或某种风格。
3. 解构应保留辨认主体的关键结构；玻璃或纸面材质应在主体通过后选择。
4. 下一轮先交付主体不同的草图与实际 48/64px 比较板，用户确认方向后再精修。小尺寸检查是必要条件，不是美观保证。
5. 建议工具组合为 Penpot（源图与曲线控制）＋可重复小尺寸渲染（resvg 或复用已有渲染依赖）＋ Android Studio（最终平台验证）。SVG-Edit 是轻量编辑备选，SVGO 是导出清理工具，不必全部安装。

依据：[Apple 图标设计课程](https://developer.apple.com/videos/play/wwdc2017/822/)、[Google 图标预览教程](https://codelabs.developers.google.com/design-android-launcher)、[Penpot](https://github.com/penpot/penpot)、[resvg](https://github.com/linebender/resvg)、[Android Image Asset Studio](https://developer.android.com/studio/write/create-app-icons)。具体工具功能与限制见上面的独立核查报告。

## 执行边界

本轮未生成新图标、未安装/克隆工具、未接入 Penpot MCP、未修改 App 或待办、未进行 Git 提交或推送。仓库研究不代表工具运行测试；后续用户识别仍需独立验收，不能由自动化取代。
