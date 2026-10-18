# Android 图片首屏启动体验调研与未完成优化建议

调研日期：2026-10-09。范围：系统启动页到海面首屏的交接、实际启动耗时及后续退潮动效。仅调研与制定建议，本轮未修改 App、待办或安装包。

## 一、结论

公开源码和产品团队案例支持组合使用三类做法：缩短首帧前初始化；仅为必要本地状态短暂保持系统启动页；先展示轻量界面，再补齐后续内容。Firefox 历史源码体现有条件保持及超时出口，Nextcloud Notes 体现先建立界面、再更新列表和图片，Google 官方 Now in Android 根据准备好的数据状态允许首帧绘制。[Firefox](https://raw.githubusercontent.com/mozilla-mobile/firefox-android/main/fenix/app/src/main/java/org/mozilla/fenix/HomeActivity.kt)、[Nextcloud Notes](https://raw.githubusercontent.com/nextcloud/notes-android/main/app/src/main/java/it/niedermann/owncloud/notes/main/MainActivity.java)、[Now in Android](https://raw.githubusercontent.com/android/nowinandroid/main/app/src/main/kotlin/com/google/samples/apps/nowinandroid/MainActivity.kt)

对未完成，推荐保留已确认的海面与退潮设计，将“海面首帧准备”从完整业务页面初始化中独立出来，尽早启动后台准备，测量并缩短必要等待。系统页负责短暂启动阶段，海面由应用首帧呈现。增加等待上限或修改背景颜色，并不会减少实际资源准备时间。

证据边界：以下商业案例是团队公开的历史优化记录，不是对当前商店 APK 的逆向结论；闭源 App 的外观不能证明其内部采用了某种技术。本轮没有实测其他产品，也未承诺未完成可以复现其改善比例。

## 二、Android 系统边界与测试入口修正

Android 12+ 在冷启动、暖启动时提供系统启动页，热启动通常没有这个阶段。系统启动页的背景是单一不透明颜色，图标有规定区域；不能按普通页面方式直接放入任意整屏海面照片。[官方 SplashScreen 文档](https://developer.android.com/develop/ui/views/launch/splash-screen)

API 33+ 的 `android:windowSplashScreenBehavior="icon_preferred"` 可请求始终显示启动图标。默认行为受到调用方是否指定 `SPLASH_SCREEN_STYLE_ICON` 的影响。未完成 minSdk 为 33，可统一评估这个属性，不需要按某台魅族做特殊分支。[官方属性说明](https://developer.android.com/develop/ui/views/launch/splash-screen)

此前录屏用 `adb am start -n` 直接启动 Activity，不完全等同用户点击桌面图标。因此应分开核查这两个入口；尚不能断言 Flyme 的全部蓝色帧都是系统页，或所有入口都不显示图标。系统页、应用窗口背景和 Canvas 纯色底恰好相近，录屏颜色本身不能区分归属。

不建议新增专门的 Splash Activity，也不建议把透明窗口、禁用预览或旧式图片 `windowBackground` 当作 Android 12+ 的通用解决方案。官方迁移指南明确警告独立 Splash Activity 可能造成双重启动页。[官方迁移说明](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate)

## 三、市面产品及官方工程怎么做

| 对照对象 | 已核实的做法 | 对未完成的参考与限制 |
| --- | --- | --- |
| Nextcloud Notes，公开源码 | 安装 SplashScreen 后建立界面；LiveData 更新笔记列表；头像加载配本地 placeholder/error | 界面、业务数据、图片可以分阶段完成；头像占位不能推断为它采用了整屏图片预览 |
| Firefox for Android，2024 年归档源码 | 特定首次启动及功能开关下等待 Nimbus 实验数据；设置最大保持时长；部分刷新和同步移到 IO | 保持要有明确条件及失败出口；它等待的不是海面照片，也不是 2026 版 Firefox 的实现证据 |
| Now in Android，Google 官方示例 | 根据 ViewModel 的 Loading/UI 状态快速判断是否继续保持 | 用独立的数据准备状态释放首帧，不反过来等待 draw |
| Zomato，2022 年官方产品案例 | Perfetto 查启动 SDK，删无用依赖，登录 SDK 按需初始化，简化布局 | 先找实际启动关键路径，推迟与首屏无关的工作 |
| TikTok，2022 年官方产品案例 | 基于 App Startup 重构初始化，简化布局，把复杂任务分散到不同帧 | 先呈现必要视觉，后续准备避免集中争抢首帧资源 |
| Meta，2025 年工程文章 | 控制启动类加载成本，使用 Baseline Profiles 和 DEX 排布优化 | 编译优化是另一层手段，需要与资源加载优化结合 |

### 开源代码证据

Nextcloud Notes 的 `MainActivity.onCreate()` 在 `super.onCreate()` 前安装 SplashScreen，然后 inflate/setContentView；本次读取的该文件没有设置 keep condition。笔记结果回调更新 adapter 并隐藏页内 progress，头像使用 Glide 本地占位和失败图片。核验对象为 `main` 分支，不能直接等同特定商店版本，也不能据此宣称整个 App 没有其他初始化。[MainActivity](https://raw.githubusercontent.com/nextcloud/notes-android/main/app/src/main/java/it/niedermann/owncloud/notes/main/MainActivity.java)、[Manifest](https://raw.githubusercontent.com/nextcloud/notes-android/main/app/src/main/AndroidManifest.xml)

Firefox 的 GitHub 仓库于 2024-06-17 归档，开发迁至 Mozilla Central。核验归档 `main` 中的 HomeActivity：首次启动标记与 Nimbus 开关控制主动延长；数据到达或 `maximumDurationMs` 到期后退出保持；部分 Pocket 设置、刷新及账户同步使用 IO 协程。它不代表后续启动没有系统页。[已读取源码](https://raw.githubusercontent.com/mozilla-mobile/firefox-android/main/fenix/app/src/main/java/org/mozilla/fenix/HomeActivity.kt)、[仓库迁移说明](https://github.com/mozilla-mobile/firefox-android)

归档 head 为 `fe8a71cd70ad5674abe1824fe11dc78372b736c2`。本次浏览工具未直接读取固定 SHA 的源文件，事实核验来自已读取的归档分支，不伪称核验了 pinned 文件。[归档提交](https://github.com/mozilla-mobile/firefox-android/commit/fe8a71cd70ad5674abe1824fe11dc78372b736c2)

Now in Android 是官方示例，不作为商业 App 案例。其 MainActivity 保持条件快速读取 `uiState.value.shouldKeepSplashScreen()`，ViewModel 初始状态为 Loading；不是等待 Compose draw 回调。[MainActivity](https://raw.githubusercontent.com/android/nowinandroid/main/app/src/main/kotlin/com/google/samples/apps/nowinandroid/MainActivity.kt)、[ViewModel](https://raw.githubusercontent.com/android/nowinandroid/main/app/src/main/kotlin/com/google/samples/apps/nowinandroid/MainActivityViewModel.kt)

### 产品团队案例证据

Zomato 案例记录平均冷启动改善约 25%，低中端设备最高约 30%；TikTok 案例记录启动时间减少 45%。这些是各自完整优化工作的结果，不能预测未完成的收益，也不能把 TikTok 的视频预加载直接当作海面首帧方案。[Zomato，2022-01-21](https://developer.android.com/stories/apps/zomato)、[TikTok，2022-09-06](https://developer.android.com/stories/apps/tiktok?hl=en)

Meta 2025 年公开文章说明类加载及初次解释执行的成本，使用 Baseline Profiles 提前编译关键路径，并配合 DEX 排布优化。可借鉴机制，不照搬它们大型应用的配置或收益数字。[Meta 工程文章，2025-10-01](https://engineering.fb.com/2025/10/01/android/accelerating-our-android-apps-with-baseline-profiles/)

## 四、未完成当前实现：已知事实与未知瓶颈

本轮只读核查当前源码。以下是代码事实，不是本轮 trace 结论。

- `MainActivity.kt` 以独立 ready 状态保持系统页；Compose SideEffect 在 `seaBitmap != null` 或进入锁定状态时释放，另有 2500ms 异常兜底。旧的“等待 draw 后才解锁”实现已移除。
- `EnvironmentResources.kt` 的 `produceState` 初值为 null，后台 `BitmapFactory.decodeResource` 使用 ARGB_8888、`inScaled=false`。目前没有提前调用 `prepareToDraw()` 或共享图片缓存。
- `UnfinishedApp` 首次 composition 中会构造 MemoryRepository、AI 配置提供者和其他 store，随后才进入图片加载声明。`produceState` 的协程并非在函数声明瞬间就执行；提前启动准备需要真正调整任务入口，不能仅重排几行调用。
- `VaultIdentityStore.getOrCreate()` 在 ID 已存在时也调用同步 `commit()`。SharedPreferences 对未改变值可能跳过实际落盘；不能据此宣称每次都写磁盘，或它是当前主要瓶颈。
- Room builder 的构造不等于主线程已执行数据库查询、迁移；没有 trace 不能把它归罪为主线程打开数据库。
- 三态海面源图约 887×1774、PNG 2.2～2.5MB。ARGB_8888 像素约 6.0MiB/张，文件压缩体积不等于解码内存。
- 源图本身小于现有测试手机的屏幕像素尺寸，不能直接建议 `inSampleSize=2` 或给所有设备统一减半。
- TidalOpening 还准备岸线、场、shader、字体和首页布局，之后执行现有退潮。首张海面可见与整段动效场景准备完成需要分别测量。
- 项目已有 release-like benchmark variant 和 ProfileInstaller 依赖。本轮未发现应用自己生成的 Baseline Profile 文件；这不代表依赖库没有附带 profile。仅依赖 ProfileInstaller 也不等于应用自己的启动 profile 已生成。

代码位置：

- [MainActivity.kt](<project-root>/android/app/src/main/java/com/unfinished/app/MainActivity.kt)
- [EnvironmentResources.kt](<project-root>/android/app/src/main/java/com/unfinished/app/ui/EnvironmentResources.kt)
- [TidalOpening.kt](<project-root>/android/app/src/main/java/com/unfinished/app/ui/TidalOpening.kt)
- [VaultIdentityStore.kt](<project-root>/android/app/src/main/java/com/unfinished/app/data/VaultIdentityStore.kt)
- [app/build.gradle](<project-root>/android/app/build.gradle)

## 五、推荐实施方案与顺序

### 1. 先补准确测量

统一记录：启动入口、必要设置读取、海面任务开始、解码完成、状态发布、系统页退场、海面绘制及首页可用。结合 Perfetto 的主线程、RenderThread 和 Compose tracing 判断延迟在哪个阶段。

分别核查真实桌面点击和命令入口、冷/暖/热启动。已有 `am start -W` 722/834ms 是上一轮两台设备的启动记录，不能当作精确海面可见时间或首页可用时间；本轮没有重跑或得出新的性能数字。

TTID 只表示首次应用 UI 帧，TTFD 才报告完整可用状态；未完成还需要自己的“有意义海面出现”时点。draw 回调可用于观测，不能成为阻止 draw 的保持条件。[官方启动指标与 Compose tracing](https://developer.android.com/topic/performance/issues/launch-time)

### 2. 独立轻量开屏入口，海面优先

在 Activity/轻量启动 ViewModel 阶段，获取必要的三态/强制夜晚/锁定状态，立即启动对应海面准备；首次 composition 只承担必要开屏视觉。首张海面就绪后允许展示，在海面已出现时继续准备首页业务、岸线、shader 等。

按测量结果推迟非开屏必要构造和初始化；本地身份仅在缺失时持久化，必须保留原有 vaultId/deviceId，不改变数据库或授权边界。锁定状态绝不能因延后检查而先显示私密首页内容。

完整退潮仍要等它需要的场景准备好，但这段准备不应继续表现为应用自己的空白纯色页。保留失败处理及有上限的异常出口，不添加固定等待来延长正常启动。[官方延迟初始化与条件 composition 建议](https://developer.android.com/topic/performance/appstartup/best-practices)

### 3. 图片优化按瓶颈选择，不先牺牲已验收设计

- 比较当前 PNG 与高质量 WebP 的文件读取、解码和视觉保真；文件更小不保证 CPU 解码更快。只在收益明确时替换。
- 可在后台解码完成后调用 `Bitmap.prepareToDraw()` 提前发起 GPU 上传。它发起异步上传，不提供“上传已完成/已上屏”的保证。[Bitmap API](https://developer.android.com/reference/android/graphics/Bitmap#prepareToDraw())
- 如完整图片确为主要等待来源，再制作由同一海面派生的轻量预览；预览与完整图保持裁切、色调和位置一致，在退潮开始前完成全质量替换。预览不是默认必做项，也不引入另一张不同构图的海面。
- 共享有限缓存可改善暖启动和 Activity 重建，冷进程的内存缓存为空，不能单独根治冷启动；引入共享缓存时必须调整现在 composition dispose 中直接 recycle 的所有权规则，避免回收仍被 Canvas 引用的 Bitmap。
- 图片采样依据实际显示需求与源尺寸，不按机型写死，也不把小于屏幕的现有源图一律降采样。[官方大图片加载指南](https://developer.android.com/topic/performance/graphics/load-bitmap)

### 4. 系统页交接只做必要收尾

评估显式启动图标及 `icon_preferred`，解决不同启动入口出现无标识空白页的风险；这属于视觉反馈与入口一致性，不是加载提速。系统页退出到海面可用短促连续过渡，避免叠加新的长品牌动画，不改变已确认的海面退潮设计。

保持条件只快速读取独立 ready 状态。AndroidX 保持条件成立时会阻止 pre-draw，等待 draw 完成再解除保持会形成循环依赖；当前版已避开这个旧错误。[AndroidX 实现](https://raw.githubusercontent.com/androidx/androidx/androidx-main/core/core-splashscreen/src/main/java/androidx/core/splashscreen/SplashScreen.kt)

### 5. 用正式性能构建验证，再补应用启动 Profile

普通 debug APK 适合功能调试，不适合直接与商店发行 App 比性能。复用现有 release-like benchmark variant：非 debuggable、可 profile、开启适当 R8 优化，仍可用现有 debug 签名，不需要为了性能调研启用正式签名密钥。[官方 Macrobenchmark 构建要求](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)

关键路径精简后，生成覆盖开屏/首页的应用 Baseline Profile，并评估 Startup Profile 的 DEX 排布收益。它们不能代替图片准备或错误保持逻辑的修复。[Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview)、[Android Calendar 官方案例](https://developer.android.com/topic/performance/baselineprofiles/case-study-android-calendar)

## 六、验收口径

1. 系统退场后的正常应用首帧已有对应三态海面，不出现应用自己额外输出的纯色等待帧；异常路径单独记录。
2. 不把系统必需的启动阶段宣称为跨所有设备可完全删除，也不将其帧误判为应用白屏。
3. 两台设备真实桌面入口与命令入口分别复核；日光、斜光、夜晚、强制夜晚、锁定、重建均不出现错误资源或隐私泄漏。
4. 报告 TTID、海面可见、首页可用的分布和退潮阶段帧耗时，使用多次样本；不以单次录屏或按钮主观反馈宣布根治。
5. 图像颜色、构图、裁切与既有退潮观感不退步；不做某台设备专用间距、图片或启动延时。
6. 同包名覆盖调试策略、原有本地数据及身份不变。任何后续修改另行实施，本报告不代表已经优化完成。

最终建议：优先执行测量与轻量开屏拆分，再按证据选择资源和编译优化。目标是让海面更早出现、系统到海面的交接连续，同时保留已确认的 UI，而不只是把白色等待改成蓝色等待。
