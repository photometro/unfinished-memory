# 未完成 C 存片 · Android 候选资源

本包只包含候选图标资源，不是 App 安装包，不会自行替换现有图标。

- 导入资源后可通过 `@mipmap/ic_launcher_fragment` 引用，但本轮未修改 App Manifest。
- 前景、背景、单色层使用 108dp VectorDrawable；核心轮廓位于中央 66dp 安全圆内。
- v26 自适应图标提供前景与背景；v33 补单色层。
- 传统图标另有 mdpi / hdpi / xhdpi / xxhdpi / xxxhdpi 五档无外裁切 PNG。
- 外部圆角或圆形裁切由 Launcher 执行，不在图层中烘焙。
- 候选资源已通过 Android SDK 35 的 AAPT2 编译与链接，但尚未在实际 Launcher 安装验收。

尺寸与平台依据：[Android 自适应图标官方指南](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)。
