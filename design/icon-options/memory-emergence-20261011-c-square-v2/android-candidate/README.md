# 未完成 · C 存片 / 近方形 v2 · Android 候选资源

本包只含图标资源，不是 App 安装包，也未修改工程 Manifest。

图标引用名 `@mipmap/ic_launcher_fragment`。前景、背景、单色层使用 108dp VectorDrawable；另含 v26/v33 自适应配置及 mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi PNG。

本版相对上一版缩小主体宽度 6%，合并轮廓宽高比调整为 0.96∶1。三层海蓝保持不变。前景放入中央安全区但不贴着边缘放大，外部圆角/圆形裁切由 Launcher 执行。

候选资源已通过 Android SDK 35 的 AAPT2 编译与链接，尚未验真机。平台结构沿用 [Android 自适应图标指南](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)。需要用户确认后再导入实际 App 并检查真机桌面，本轮未安装。
