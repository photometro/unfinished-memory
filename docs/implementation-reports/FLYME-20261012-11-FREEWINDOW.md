# 第11条续验：Flyme 自由窗比例与首页底部按钮

日期：2026-10-12。范围：用户授权两台真机进一步分析／调试，并在测试中报告小窗按钮上移。第11条仍为 `pending-validation`；不启动第12条。

## 结论

两个问题属于不同层级，已分别修复并部署标准 Debug 包到两台手机：

1. **细长小窗**：Flyme 将未完成按保持全屏比例的兼容策略处理；其逻辑窗口没有变矮，只缩放了 Surface。有效方案是声明本机框架实际识别的 application 元数据 `window_mode_un_resize=false`，使其走可调整逻辑窗口的分支。不是给魅族21／18s单独硬编码宽高。
2. **按钮跑到文案下方**：原首页在短屏／大字号分支中，把底部弹性空间替换为固定32dp，同时滚动容器没有最小视口高度。窗口真正变矮后，该分支被激活，即使内容放得下，也丢失了底部锚点。改为“滚动内容至少占满可用视口＋正文／底部操作两段 `SpaceBetween`”：内容放得下时按钮靠底；内容超过窗口时纵向滚动，不覆盖正文。

海图首帧的半透明启动主题保留；不更换图标、图片、字体或业务状态。正式发行配置没有同步 OEM 元数据，当前只验证 Debug 的启动组合。

## 两台设备与窗口对照

尺寸是 `CurrentConfiguration.mBounds` 的**逻辑窗口像素**，不是桌面上缩放后的小窗物理大小。宽高比／设备密度由运行时读取。

| 设备 | Android API | 未完成原包 | 观己对照 | 未完成最终包 | 最终小窗 H/W |
|---|---:|---:|---:|---:|---:|
| 魅族21 | 36 | 1080×2340 | 1080×2023 | 1080×2023 | 1.87315 |
| 魅族18s | 33 | 1440×3200 | 1440×2703 | 1440×2703 | 1.87708 |

两机分别验证 `flyme-pinned-window` 和展开后的 `flyme-mini-window`。最终从系统小窗底部控制条下滑返回全屏，恢复1080×2340／1440×3200，PID保持不变，没有通过重启冒充窗口切换。

魅族21原包当前任务的 `mScreenRatio=true`、`mUnResizeFlags=1`；有效声明后当前任务变为 `false`／`0`。旧 recent task 仍可能保留旧值，因此不能只搜索整段 recents 后任取一行。最终 Activity 保持 `occludesParent=false` 和 `RESIZE_MODE_RESIZEABLE`，海图启动主题未撤回。

## 根因证据：本机框架，而非猜测属性

只读拉取两机 `/apex/com.flyme.runtime/javalib/service-flyme.jar`，使用 SDK `dexdump` 检查相关分支，没有 root、修改系统文件、反射调用私有方法或绕过安装限制。

魅族21：

- `FlymePackageCategoryCache.getUnResizeAppFlagInner` 读取 `ApplicationInfo.metaData["window_mode_un_resize"]`；布尔 `true` 返回2，`false` 返回−2，缺失返回−1。
- `FlymeWindowModeConfig.isUnResizeApp(String)` 对小于−1的值直接返回false；否则还可能使用厂商名单／游戏分类等判断。
- `isUnResizeApp(Task, boolean)` 将结果缓存为 `mUnResizeFlags` 的0／1。它是此分支的布尔缓存，不能擅自解释为“半透明位掩码”。
- 小窗信息创建将上述结果用于 `mScreenRatio` 选择。魅族18s框架存在同样的 application metadata 读取机制，真机结果也一致。

本轮已确定兼容比例分支及显式声明的因果效应；**没有进一步证明缺失声明时究竟由游戏识别、运行时名单还是其他分类来源将该包归入此策略**。不得写成“已确定被错分为游戏”。

二进制证据位于 `debug/FLYME-20261012/meizu21-flyme-dex/` 和 `meizu18s-flyme-dex/`，重点文件为 `com_android_server_wm_FlymePackageCategoryCache.dexdump.txt` 与 `com_android_server_wm_FlymeWindowModeConfig.dexdump.txt`。

该元数据是本轮设备框架实现识别的厂商约定，不是 Android 公共标准，也不保证所有 Flyme 或未来系统均支持。普通 Android 仍依赖标准可调整 Activity 与按实际约束布局。

## 实验与排除

| 实验 | 实际结果 | 判定 |
|---|---|---|
| 原包＋系统最近任务菜单进入小窗 | 双机逻辑窗口仍为全屏比例 | 红基线成立 |
| force-stop 后用 `0x18000000` 创建新 task | 魅族21 taskId变化，仍1080×2340 | 不是仅清掉旧任务就能解决 |
| 只把 `windowIsTranslucent` 改为false | APK资源与运行时 `occludesParent=true` 证实生效，仍1080×2340 | 半透明不是充分根因，不采用该方案 |
| 正常 Debug manifest 加入 application 元数据 | 两机变为与观己相同的逻辑尺寸 | 有效 |
| 魅族21覆盖回原包，再覆盖元数据修复包 | 2340 → 2023，故障随配置回退／恢复 | A/B回退对照通过 |
| 最终包保留元数据并修改首页视口规则 | 比例保持正常，按钮回到底部 | 两层修复兼容 |

无效样本单独保留，不纳入正反证据：`--activity-new-task` 是本机不支持的命令；早期 `metadata-debug.apk` 未实际合并元数据；一些坐标点击没有进入小窗、采集到fullscreen。以脚本的 `validSample`、实际 mode／可见性、PID和当前配置筛选。名为 `fixed-full-return` 的早期采样实际上是控制条上滑后回到 pinned，不是全屏；最终全屏证据为 `final-home-full`。

## 底部按钮的红绿回归

新增 `HomeViewportLayoutTest` 使用合成页面状态，不调用 Repository，不保存记忆。

修复前在 API35 模拟器运行 `shortEmptyHomeKeepsActionsAtViewportBottom`：真实失败，按钮距可用视口底部 **240.38095dp**；日志为 `home-regression-red.txt`。

修复后6项全部通过（`home-regression-green.txt`）：

1. 短窗无记录空态，按钮底部锚定。
2. 短窗安静态，按钮底部锚定。
3. 短窗有遇见且有Cue，底部按钮及遇见操作均可见。
4. 大字号但内容仍放得下，不能无故把按钮移上去。
5. 同一 composition 在760→600→760dp约束下往返，按钮仍靠底。
6. 320dp极短窗／1.5字号／有遇见：正文与按钮不覆盖，两项操作可滚动抵达且回调有效。

既有 `OpeningAndHomeLayoutTest` 9项全部通过，包括自动进入首页、文案交接、两操作、玻璃透明度、内容轴、键盘避让和大字号可达性（`opening-home-regression.txt`）。JVM6类16项全部通过。构建命令：

```powershell
$env:JAVA_HOME='%JAVA_HOME%'
.\gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

Compose测试运行在已有API35本地模拟器。魅族21安装 `com.unfinished.app.test` 辅助包返回 `INSTALL_FAILED_USER_RESTRICTED`，没有任何真机 instrumentation 测试执行；未反复请求人工放行、未绕过限制。真机用同包主App和系统UI操作／窗口读取／可访问性树／截图验证，不将模拟器测试说成真机仪器结果。

## 真机按钮位置证据

最终 `final-home-mini` 均为当前运行且可见的 `flyme-mini-window`。

| 设备 | 左按钮 bounds | 右按钮 bounds | 按钮底边距安全区底部 |
|---|---|---|---:|
| 魅族21 | [66,1772][618,1904] | [646,1772][1014,1904] | (1954−1904)/2.75 ≈18.18dp |
| 魅族18s | [96,2339][821,2531] | [861,2339][1344,2531] | (2603−2531)/4 =18dp |

坐标来自App可访问性树，仍是逻辑坐标，不能直接当成系统已缩放的触摸坐标。底部包含16dp页面留白和2dp尾留白／像素舍入。魅族21修复前小窗按钮底边为985px，空余区域并非空间不足，截图与控件树均证实上移。

## 本轮改动与部署

- `android/app/src/debug/AndroidManifest.xml`：application层元数据。普通Debug生效；main／release manifest未加此声明。
- `android/app/src/main/java/com/unfinished/app/ui/NativeHome.kt`：通用首页布局改为滚动＋最小视口高度＋两段SpaceBetween，新增 `home.viewport` 测试定位；不再用700dp阈值决定按钮是否靠底。开屏的独立紧凑布局没有改动。
- `android/app/src/androidTest/java/com/unfinished/app/ui/HomeViewportLayoutTest.kt`：6项回归。

最终主APK：`debug/FLYME-20261012/fixed-home-debug.apk`。

SHA-256：`0EEB9D91C5D82BD1B8194FE5E76FDC72CA28BD7FC60EF2B7BF03E2EFF299C6ED`。

两机均 `adb install -r` 返回Success；包名 `com.unfinished.app`、versionCode4、versionName0.4.0，沿用Debug签名。firstInstallTime仍为魅族21的2026-09-09 06:47:58／魅族18s的2026-10-08 20:26:12。未卸载、未清数据、未读取正文数据库、未输入／保存内容；没有做逐行数据校验，不能将安装时间不变说成逐行数据完整性证明。未测温度。

## 复跑与证据入口

设备已实际进入小窗时运行下列只读反馈命令。它在原包会返回 `RED_FULLSCREEN_RATIO`／非零断言退出，在修复包返回 `GREEN_SHORT_WINDOW`；未运行／不可见／fullscreen不会误报绿色。

```powershell
& '<project-root>\debug\FLYME-20261012\flyme-window-probe.ps1' `
  -Serial Meizu21 -Label verification -Screenshot -AssertShortWindow
```

主证据目录：`debug/FLYME-20261012/device-evidence/`。重点为 `baseline-mini`、`guanji-mini`、`opaque-mini`、`regression-A-return`、`regression-B-return`、`final-home-mini`、`final-home-full`，每个目录保留当前任务／窗口、JSON和截图。最终截图已目视检查，按钮在底部、文案无裁切。

## 边界、剩余验收和防复发

- 已验证这两台设备当前系统，不声称所有OEM／未来Flyme均根治。
- 无记录与安静态在双机真实页面验证；有遇见、大字号、极短窗口在合成Compose测试验证，不在用户数据库造事件。
- 请用户目视确认最终小窗的按钮位置／留白；第11条最终签收及历史性能长尾／Macrobenchmark未因本轮自动完成。
- 正式发行采用的启动主题及OEM元数据组合需单独验收；本轮不扩大到发行或后续任务。
- 防复发重点：先验证真实可见窗口的逻辑尺寸，而非旧recents或截图大小；滚动兜底和底部锚定不应由同一个粗阈值捆绑。布局回归须同时测试“短而放得下”和“真正溢出”。
- 临时单变量构建脚本已在 `.codex-tmp/flyme-window/README.md` 标记为诊断输入，不进入普通构建；未加入生产调试日志。本轮模拟器已关闭、明确创建的临时目录已清理，已有AVD保留。设备上的两份临时探针文件已移除，F盘证据保留；双机最终停在修复后小窗首页供目视验收，见 `final-review`。
- 本轮不提交／推送Git，也不上传知识库。报告记录不代表用户已签收。
