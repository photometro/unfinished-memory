# 未完成 · 未字印记

本轮是按用户提供的流程重新探索的一个独立候选，不接续照片、册子、漂流瓶，也不预设解构/玻璃。**未定稿、未改 App 资源、未构建安装到设备。**

## 设计主张

核心价值是保存生活片段，在未来重新遇见过去。选用名称核心字“未”建立身份，而不是把每种记忆媒介或海洋景观画进图标。温润笔画和轻微回弯只辅助传达生活还在延续，不要求用户从曲线猜算法。

定制字形是路径，不依赖系统字体。采用背景 `#315966` 与字形 `#F2EADD` 两色；识别不依赖渐变、纹理或玻璃高光。**这是一条名称识别优先的设计路径，不能声称陌生人已能据此猜中功能类别。**

## 实际执行的流程

1. 设计说明：见 BRIEF.md，明确价值、用户假设、气质与混淆风险。
2. 同类与指南研究：见 research.md；实际核对了官方商店图标及 Day One 官方历史截图，区分观察与推断。
3. 草图：未字标、记忆匣、回返印记三种黑白轮廓。
4. 小尺寸筛选：见 concept-check.png / SELECTION.md；48/64 px 主代理自审，不是假称用户测试。
5. 精修：内置 imagegen 提供字形精修稿，随后可编辑 SVG 复绘与平台比例整理；最终 SVG 是资源源头，不是位图无损矢量化。
6. 适配候选：独立前景/背景/单色 SVG、Android XML、传统密度 PNG、商店 512 PNG。静态 XML/几何一致性校验，不代表编译、Launcher 或真机验收。
7. 环境验收：目前只有圆形/圆角、主题色和浅深背景模拟；桌面模拟中的邻近图标为明确标注的占位图，不是真机截图或同类产品对比证据。真实用户“类型判断/再找到/混淆”测试尚未执行。
8. 交付：候选源文件与资源在此，不宣称已定稿。需用户认可方向后，才继续真机与识别测试，再决定是否替换。

## 文件

- icon-master.svg：完整可编辑源图，108×108 视口，无预先圆角遮罩。
- foreground.svg / background.svg / monochrome.svg：独立分层。
- icon-1024.png / store-512.png：按可见 72dp 视口导出的候选大图与商店图，不预画圆角。
- imagegen-refinement.png：生成阶段精修稿；与 SVG 为同一设计方向，但不是逐像素相同的图形。
- android-candidate/res：候选 Android 资源包；未复制进 app/src/main/res，未修改 Manifest。
- preview.png：小尺寸、裁切与主题预览。
- safe-zone.png：108dp 画布与中央 66dp 安全范围示意；虚线只在检查图上，不属于交付前景。
- desktop-simulation.png：浅深背景下的桌面模拟。
- concept-sheet.png / concept-check.png：精修前的轮廓探索与检查。
- PROMPTS.md：完整生成提示词与内置工具方式。
- QA.md：检查范围、数据和未完成验收。

## 平台规格与证据

Android 官方当前要求彩色自适应图标有前景/背景、主题版提供单色层，每层 108×108dp，核心图形在中央 66dp 安全区；系统负责裁切。资源包按此制作，但仍需实际 Launcher 验收。[官方 Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)。

Google 设计教程建议简化形状、减少效果和小字、检查对比与单色轮廓；这是本轮的小尺寸策略依据，不是用户喜好证明。[官方教程](https://codelabs.developers.google.com/design-android-launcher)。
