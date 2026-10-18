# 图片首帧的非标准启动路径：源码复核与试验建议

日期：2026-10-09。对象：未完成 Compose 原生版。状态：研究完成、尚未实施或真机验证。

本轮仅查阅资料、读取项目并保存本报告，没有修改 App、待办、签名或安装包，也没有操作手机。本报告补充 `startup-market-research-20261009.md`，不撤销既有视觉设计。

## 1. 结论与此前表述的修正

“标准 SplashScreen 不支持任意整屏图片背景”不等于“技术上完全不能绕过系统纯色启动页”。存在有源码依据的非标准路径：以透明 Activity 启动，让系统不创建通常的 Splash 窗口，再由应用呈现海面图片。

需要区分两个目标：

- **首个应用画面就是海面，中间不插入纯色 Splash**：有可试验路径。
- **图标按下后立刻出现海面，连进程创建和图片准备的等待都没有**：这条路径不能保证；准备期间可能仍显示桌面或上一画面。

目前是 AOSP 行为和工程推导，不是魅族 21、魅族 18S 或其他厂商系统上的通过结论。不承诺所有启动入口、系统版本都能消除全部过渡画面。

## 2. 关键证据

### 2.1 透明启动的例外仍存在

核查 Android 13 与 Android 16 的 `ActivityRecord.validateStartingWindowTheme()`：透明或悬浮主题会被拒绝创建通常的 Splash；单独的 `windowDisablePreview` 对 Launcher 等系统来源不具有同样效果。因此透明入口比仅禁用预览更值得验证。最近任务恢复另有 snapshot 分支，不能推断它也全部消失。[Android 13 源码](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android13-release/services/core/java/com/android/server/wm/ActivityRecord.java)、[Android 16 源码](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/services/core/java/com/android/server/wm/ActivityRecord.java)

网上原作者在 2021 年给出了透明主题和透明代理 Activity 的可复制实践，并明确提示不是官方承诺。该历史实验可以作为线索，不作为当前 API 33/36 或 Flyme 的兼容性证明。[原作者实验](https://takusan.negitoro.dev/posts/android_12_disable_splashscreen/)

### 2.2 可以使用公开 API 恢复不透明

`Activity.setTranslucent(false)` 从 API 30 起公开提供透明转不透明能力，并返回转换是否成功；本项目最低 API 33 满足调用条件。它不是简单改颜色，也不能用 `setTheme()` 替代服务端窗口状态转换。此方案不需要 Root、隐藏 API 反射或降低 targetSdk。[公开 API](https://developer.android.com/reference/android/app/Activity#setTranslucent(boolean))、[转换实现](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android16-release/core/java/android/app/Activity.java)

### 2.3 仅换 PNG 背景或退出回调太晚

普通新式系统 Splash 处理 `windowBackground` 时会提取代表颜色，而不是直接展示整张照片；旧式兼容分支另当别论。把照片塞进启动图标也受图标尺寸和遮罩约束，不是整屏背景方案。[Android 13 Splash 绘制源码](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android13-release/libs/WindowManager/Shell/src/com/android/wm/shell/startingsurface/SplashscreenContentDrawer.java)

在 Splash 退出回调里加图片发生于退出阶段，无法倒改已经显示的系统首帧。此外 AndroidX 的 keep 条件通过阻止 pre-draw 工作：透明实验如果照搬整窗等待，仍会阻止应用首绘。[AndroidX 源码](https://raw.githubusercontent.com/androidx/androidx/androidx-main/core/core-splashscreen/src/main/java/androidx/core/splashscreen/SplashScreen.kt)

## 3. 可试验路线比较

| 路线 | 设计 | 判断 |
| --- | --- | --- |
| A：同 Activity 透明启动 | 启动主题透明；首个可见应用画面完整显示本地海面；之后调用公开 API 转不透明 | 优先做最小试验，少一次页面跳转；必须检查转换是否成功及任务恢复 |
| B：透明代理入口 | 无可见内容的透明 Activity 启动同任务栈的不透明 Main，再结束代理 | 对长期 Main 窗口影响较少；多一个 Activity，需要复核返回栈、重复启动、深链和二次 Splash |
| C：只加 disablePreview | 不改窗口透明性 | 不能作为桌面启动的可靠解法 |
| D：PNG windowBackground、巨型图标或退出时替换图片 | 仍保留通常的系统 Splash 创建路径 | 不等价于系统首帧整屏照片，不优先投入 |

B 的推导条件是先启动 Main 再结束代理、不使用 NEW_TASK/CLEAR_TASK、不强制新任务栈；这些条件降低二次 Splash 的可能，不能代替 ROM 实测。可在来自应用的第二段 Main 入口另评估 disablePreview，不能把它当桌面入口的保证。[任务选择与启动源码](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-13.0.0_r1/services/core/java/com/android/server/wm/ActivityStarter.java)

不优先用 `windowIsFloating`：它同时改变窗口尺寸、布局 flags 和默认 dim，副作用超出启动屏开关。[PhoneWindow 实现](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-13.0.0_r1/core/java/com/android/internal/policy/PhoneWindow.java)

## 4. 针对当前项目的改造边界

本轮读取结果：`android/app/build.gradle` 为 minSdk 33、targetSdk 36，依赖 core-splashscreen 1.0.1；Launcher 直接进入固定 portrait 的 MainActivity。API 31+ 启动主题继承 Theme.SplashScreen，系统和应用窗口底色均为 opening_startup_background。

`MainActivity.kt` 当前通过 startupVisualReady 保持系统页，海面 Bitmap 在 Compose 内异步准备；有 2500ms 兜底。海面就绪后才启用后续潮滩图片加载。当前不是透明启动实验，也不能据此称绕开方案已实现。

试验应遵守以下边界：

1. **先拆首屏资源准备，不先初始化完整业务 UI。** 海面使用打包的本地资源；首屏所需之外的数据、图片和业务初始化尽量后移。当前读取不能证明 Repository 或 AI 配置构造已经造成实际阻塞，需要测量。
2. **不保留会阻止首绘的整窗 keep 门控。** 透明时等待应由应用首屏状态负责；不得一边阻止绘制、一边等待“已绘制”回调。
3. **透明不是长期页面风格。** 海面完整不透明绘制，确认图片接管后再转换窗口；转换失败要记录并有退出/恢复路径，不无期限停在透明窗口。
4. **沿用现有海面与退潮。** 启动接管成功才开始退潮，不改母图、主页构图，不加独立品牌页或固定额外等待。减弱动态设置优先。
5. **锁定和异常单独处理。** 锁定时不展示私人内容；图片失败不能无限等待，也不能为了“没有色帧”隐藏异常。正常图片首帧和故障兜底分开验收。
6. **不按某台设备写延时和坐标。** 沿用真实窗口尺寸、安全区和图片裁剪规则，复核三种光照主题、强制夜间、不同宽高比与字体设置。

## 5. 验证方案与通过条件

需用户批准后才实施/安装。用可独立选择的实验构建保留标准启动作为对照；不删数据、不更换正式签名、不修改系统配置。若采用隔离包，提前说明额外包用途及厂商可能要求安装确认；若采用已授权的同包调试覆盖，则保持包名和签名一致。

对照三组：现有标准方案、A 同 Activity 转换、必要时 B 透明代理。先测 A，若边界失败再考虑 B，不同时大改整套 UI。

| 类别 | 复核内容 |
| --- | --- |
| 入口 | 真实桌面点击、电脑命令、通知/外部入口（项目具有时）；分开记录，不相互替代 |
| 生命周期 | 无进程冷启动、进程存活 Activity 重建、热返回、最近任务恢复、快速重复点击 |
| 窗口 | 横屏桌面进入 portrait、状态/导航栏、安全区、多窗口支持边界、返回栈与任务快照 |
| 资源 | 日光/暮色/夜晚、首次冷缓存、慢设备、图片失败、锁定、减弱动态 |
| 系统 | 两台现有手机的实际版本；补 AOSP API 33 与 API 36 验证系统差异，不把手机版号当 Android 版本 |

桌面启动必须真实点击验证：AOSP 将 Home/SystemUI 与 shell 启动来源分别处理，电脑命令隐藏预览成功不能证明桌面成功。[入口分类实现](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android13-release/services/core/java/com/android/server/wm/ActivityRecord.java)

记录从点击到海面可见、到可交互的耗时，图片前白/黑/纯色帧，桌面停留，窗口转换及退潮接管是否闪动；结合逐帧录像与系统 trace 判断帧归属，不能只凭颜色猜。

通过条件：正常冷启动中首个应用内容画面是正确裁剪的海面，没有新增可见白黑底；退潮连续；总等待与交互响应不因隐藏占位变差；返回、恢复、锁定及系统栏不回归。最近任务 snapshot 若存在，应单独说明，不能把冷启动通过写成“任何入口永远只有海面”。

## 6. 决策状态

**可进行最小验证，不应直接宣布生产可用。** 非标准透明入口有现行源码依据，恢复不透明还有公开 API；最主要的不确定性是实际厂商行为、启动等待观感以及恢复/任务栈边界。待用户批准试验并通过验收后，再决定是否取代标准路径。
