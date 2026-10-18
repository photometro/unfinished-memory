# 未完成：App 图标设计原则复核

日期：2026-10-11

## 本轮边界

本轮仅研究设计原则与辅助项目，不生成新图标、不安装软件、不替换 App 资源、不修改待办。官方规范检查与审美、识别测试分开：安全区合规不等于设计通过。

## 一、设计原则

### 1. 先有主体，再有风格

Apple 的 App Icon Design 课程把隐喻、简洁、情感联系和身份延续作为设计基础，也建议先画草图、比较同类产品，再测试。物品或抽象标记都可以；不要求每个功能都画进去。对本项目的判断：产品主语是“保存生活片段、未来重新遇见”，海洋是表达语言。

来源：[Apple App Icon Design，WWDC17 官方课程及文字稿](https://developer.apple.com/videos/play/wwdc2017/822/)。本轮读取课程文字稿；其平台外观实例较早，只采纳通用设计方法。

### 2. 简化细节，保留可辨认的形状

Google 建议减少复杂形状、文字和密集效果，保证前景与背景对比，并优先使用便于编辑的矢量素材。对本项目的判断：解构可以改变位置和组合，但不能把关键轮廓打碎成只能依赖说明理解的碎片。

来源：[Google Design and preview your app icons](https://codelabs.developers.google.com/design-android-launcher)。页面注明 2022 年更新；其中旧尺寸示例不作为本轮资源参数。

### 3. 独特性不等于陌生和复杂

Apple 建议比较同类 App，在主屏、文件夹等小尺寸环境检查形状、对比和构图。对本项目的判断：需要一个可记住的轮廓，而不是靠纹理、标题或叙事解释勉强辨认。首次猜到产品类别与熟悉后认出品牌是不同验收项，不能承诺陌生人仅凭新图标猜出“未完成”这个名称。

来源：[Apple App Icon Design](https://developer.apple.com/videos/play/wwdc2017/822/)。最后一句为本项目识别测试口径，不是官方量化保证。

### 4. 材质服务形状，不代替形状

Apple 2025 图标课程说明，系统玻璃效果本身会增加细节，应减少源图里重复烘焙的效果，保留较清楚、粗壮的形状。该内容适用于 Apple 的新图标渲染体系，不是 Android 强制规范。

本项目判断：玻璃、纸面、解构都只是候选表现手法，不是必选项；不能把 Android UI 的液态玻璃组件直接当作桌面图标的动态材质。当前不预设采用哪一种风格。

来源：[Apple Say hello to the new look of app icons，WWDC25](https://developer.apple.com/videos/play/wwdc2025/220/)。

### 5. 小尺寸和设备裁切是前置检查

彩色 Android 自适应图标由前景和背景组成，每层 108×108dp，关键标志应保留在中央 66×66dp 安全区域；主题着色另提供 monochrome 图层。不得在资源外轮廓提前烘焙系统裁切或外部阴影。矢量是推荐格式，不是唯一合法格式。

来源：[Android Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)。尺寸以此页面为准，不能用旧教程的 72px 表述替代。

### 6. 商店图标和桌面图标是不同交付物

Google Play 商店图标为 512×512px、32-bit PNG、sRGB、最大 1024KB 的完整方图，平台处理外部圆角与阴影；允许图形内部的光影。不要把提前裁好的圆角图当成自适应前景层或商店母版。

来源：[Google Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)。

## 二、对上一轮的复盘（设计判断）

- 工程检查通过得太早成为了选定理由；它只能说明资源不被裁坏，不能说明构图漂亮。
- 把“符号可以解释为未完成”误当成“用户愿意看、能够记住”。
- 材质、纹理和故事描述先于主体判断，导致大图精致也无法挽救轮廓。
- 需要真正不同的主体草图，而不是同一图案反复调整粗细、缺口、颜色。

这些是基于用户反馈和当前设计过程的判断，未进行独立用户识别实验。

## 三、下一轮建议的验收门槛

1. 先交付多个主体/轮廓不同的粗草图，不先交付精修大图；暂不固定为字、纸船或海浪。
2. 同一张评审板展示原尺寸、48px、64px和单色轮廓；48/64是本项目比较尺寸，不是统一系统标准。
3. 先由用户确认视觉方向；没有通过就不做材质精修和 Android 导出。
4. 通过后才调整曲线、光学中心、留白和材质，并检查圆形/圆角裁切、主题着色、浅深壁纸。
5. 后续识别测试分开问“像哪类 App”和“看过后能否再次找到”；不能把我的自评当成用户测试。

参考：[Apple 小尺寸与桌面测试建议](https://developer.apple.com/videos/play/wwdc2017/822/)、[Google 图标预览教程](https://codelabs.developers.google.com/design-android-launcher)、[Android 图标制作工具](https://developer.android.com/studio/write/create-app-icons)。

## 四、资料限制

本轮尝试打开 Apple HIG App icons 页面，但文本抓取只返回需要 JavaScript 的提示；未声称通读该动态页面。Apple 原则主要以能读取完整文字稿的官方课程复核。GitHub 辅助项目另见本目录 github-tools.md；仓库研究不代表已经安装、运行或完成兼容性实测。
