# 第11条续验：Flyme小窗返回全屏的动态渲染修复

日期：2026-10-12。范围：魅族21、魅族18s，现有普通Debug包 `com.unfinished.app`。

## 结论与验收状态

已在两台真机复现用户报告的按钮重影，并在魅族21复现白色横条。完成正向、反向A/B验证后，修复版已同包覆盖到两机，未卸载、未清数据。最终每台连续3轮小窗返回全屏：有效窗口切换全部成立，检查区间共740个原始录屏帧，未检出白条或重复按钮；关键帧人工图像复核未见之前的重复背景块。背景取景连续性另以实际Compose渲染的红／绿回归验证。

本轮修复与工程验证通过，仍请用户目视复核正常操作下的切换观感。不能据此宣称所有Flyme版本、所有窗口操作或第11条全部验收完成；第11条继续 `pending-validation`，不启动第12条。

用户在本轮开始时已确认：上轮修复后，小窗的底部按钮位置正常。本轮保持该布局，不重新移动按钮。

## 复现与调查

设备：

| 设备 | Android API | 全屏逻辑尺寸 | 小窗逻辑尺寸 |
| --- | --- | --- | --- |
| 魅族21，Meizu21 | 36 | 1080×2340 | 1080×2023 |
| 魅族18s，Meizu18s | 33 | 1440×3200 | 1440×2703 |

通过系统最近任务菜单进入Flyme小窗，再展开小窗、拖动窗口控制条返回全屏。录像前后同时读取该应用的任务窗口模式及PID，排除没有真正进入小窗、只看到历史任务、切换时进程重启等无效样本。

调查开始前的可证伪假设：

1. 背景或液态玻璃的旧尺寸离屏层在窗口变化时残留。
2. 半透明Activity与Flyme窗口交接产生旧画面残留。
3. 尺寸／insets与窗口配置交接不同步。
4. 背景按每一帧的窗口比例重新cover取景，造成视觉地标跳动。

定向生命周期日志证实：两机缩放前后都是同一Activity实例，仅窗口高度变化，没有Activity销毁重建。因此**不把Activity重建当作本轮原因**。SurfaceFlinger中观察到对应任务的snapshot animation图层；结合录屏可推断窗口交接存在旧画面残留，但没有完整逆向OEM合成器，不将此推断写成已证明的系统内部机制。

## A/B及反向验证

以下数量仅统计视频相对时间1.5～3.8秒内的原始帧；采用保留原始PTS的抽帧，不按固定fps补帧。白条与重影是两个独立断言，不能混成一个结果。

| 实验 | 魅族21 | 魅族18s | 判断 |
| --- | --- | --- | --- |
| 原始基线 `baseline-1` | 白条48帧；重复按钮10帧 | 白条0帧；重复按钮21帧 | 原问题可复现，检测器可失败 |
| 仅改不透明Activity `opaque-1` | 白条49帧；图像复核仍有重影 | 图像复核仍有重影 | 不能通过取消半透明启动解决 |
| 仅取消玻璃背景采样 `no-glass-1` | 白条49帧；图像复核仍有重影 | 图像复核仍有重影 | 玻璃不是独立根因 |
| 无玻璃条件下直接绘制背景 `direct-1` | 白条未出现；重影仍在 | 重影仍在 | 不能当作完整修复；白条具有间歇性 |
| 恢复原视觉层，仅声明尺寸配置处理 `config-1` | 白条0帧；重复按钮0帧 | 白条0帧；重复按钮0帧 | 保留玻璃及半透明启动也可消除本轮残留 |
| 回装无配置声明的诊断基线 `reverse-1` | 白条0帧；重复按钮1帧 | 白条0帧；重复按钮6帧 | 反向恢复重影，支持配置声明的因果作用 |

反向试验没有恢复白条，必须保留这个事实：白条并非每次都出现。本轮最终六次复测未再出现，不构成“永不出现”的保证。

## 实施内容

### 1. 尺寸变化由现有Compose界面实时消费

MainActivity仅声明：

```xml
android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
```

该声明经正反对照消除了本轮窗口交接重影。没有关闭全部配置变更；语言、density、fontScale、uiMode仍保留通常的Activity重建行为。界面继续以真实窗口测量和 `LocalConfiguration` 更新，声明本身不能代替响应式布局或状态保存。Android官方说明了[多窗口切换的配置变化](https://developer.android.com/develop/ui/compose/layouts/adaptive/support-multi-window-mode)及[自行处理配置变化所承担的更新责任](https://developer.android.com/guide/topics/resources/runtime-changes)。

### 2. 潮汐树采用稳定取景，不随短窗口重新居中

过去的 `drawCover` 使用当前窗口宽高比：窗口由短变长时重新计算图片裁切中心，造成同一处树枝突然移动。

现在以 `WindowManager.maximumWindowMetrics` 的比例作为**纯视觉参考**，计算：

```text
sceneFrameHeight = max(actualHeight, actualWidth / referenceAspect)
```

短窗口仍绘制同一取景基准的海面／潮滩，超出真实窗口部分裁掉；恢复全屏时逐步露出，不重新拉伸或居中。Shader的viewport和Reveal Field也沿用这一视觉基准。缺少有效参考比例时退回实际窗口尺寸，横向、不同宽度和比参考更高的窗口均有独立单元测试。

实际布局、insets、滚动和点击区域仍来自当前窗口。没有用最大屏幕高度摆按钮，没有魅族型号分支或1080／1440等产品布局常量。测试脚本里的系统手势坐标仅用于本轮操作两台测试机，不进入App代码。

### 3. 保留项与诊断清理

- 保留既有小窗底部锚点及内容溢出滚动规则。
- 保留用户认可的海图半透明Debug启动、自动退潮、当前图标及玻璃按钮。
- 背景离屏层及玻璃采样恢复正常，未用关闭效果掩盖问题。
- 移除生命周期探针日志和无玻璃／直接绘制实验分支；各诊断APK仅留在本地证据目录，不是最终候选。
- Flyme `window_mode_un_resize=false` 仍仅在原Debug manifest中；本轮未擅自推广到release。

涉及源码：

- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/java/com/unfinished/app/ui/TidalOpening.kt`
- `android/app/src/main/java/com/unfinished/app/ui/SceneFrame.kt`
- `android/app/src/test/java/com/unfinished/app/ui/SceneFrameTest.kt`
- `android/app/src/androidTest/java/com/unfinished/app/ui/SceneResizeContinuityTest.kt`

## 验证结果

### 实际渲染红／绿回归

`SceneResizeContinuityTest.changingWindowHeightDoesNotReframeTheShore` 使用带位置差异的合成Bitmap，通过真实OpeningScene绘制200×400dp → 200×350dp → 200×400dp，并比较共同可见区域像素；不是只测计算函数。

- 修复前：失败，最大像素差 `0.05098042`，超过 `3/255` 的容差。
- 修复后：通过；同文件的已安装Launcher配置白名单断言也通过。
- API35模拟器：新增2项 + 首页视口6项 + 开屏／首页9项，合计 **17项通过**。
- JVM：**21项通过**，失败／错误／跳过均为0。
- `assembleDebug`、`assembleDebugAndroidTest`、`testDebugUnitTest`离线构建成功。

Instrumentation辅助包仅安装到本轮启动的API35模拟器，未安装到两台手机。测试后已关闭该无界面模拟器。

### 最终双机连续复测

| 设备 | 轮次 | 检查原始帧 | 白条帧 | 重复按钮帧 | 小窗→全屏有效 |
| --- | --- | ---: | ---: | ---: | --- |
| 魅族21 | fixed-1 | 98 | 0 | 0 | 是 |
| 魅族21 | fixed-2 | 100 | 0 | 0 | 是 |
| 魅族21 | fixed-3 | 100 | 0 | 0 | 是 |
| 魅族18s | fixed-1 | 147 | 0 | 0 | 是 |
| 魅族18s | fixed-2 | 148 | 0 | 0 | 是 |
| 魅族18s | fixed-3 | 147 | 0 | 0 | 是 |

检测白条采用局部近白色横向覆盖比例；重复按钮采用左右文字共同出现的纵向带，并排除本轮暗色Launcher露出区域。阈值已先对原始失败样本及反向样本验证。这是针对本轮视觉缺陷的回归探针，不是通用OCR或所有壁纸下的识别器；结合原始关键帧、底部逐帧拼图、背景场景拼图复核，不能代替用户在设备上观看完整动效。

### 冷启动回归

两机均force-stop现有App后启动：魅族21通过明确MainActivity入口，魅族18s点击已观测的桌面图标。录像显示海面开屏、退潮并自动进入首页，两颗底部操作正常；没有为了本轮修复改成不透明纯色启动。

录像辅助脚本曾因PowerShell进程ExitCode空值误报失败，后核对录屏已正常结束、stderr为空，手工取回并完整解码视频，再复核最后首页帧及任务全屏状态。修正了辅助脚本空值判断；不把脚本报错混同成App失败。录屏静止后不持续产生帧，实际视频PTS终点分别为4.051433s、5.180733s，不用10秒墙钟等待推算帧数。

## 部署与证据

最终两机同包覆盖的是：

`<project-root>\debug\FLYME-20261012-RESIZE\fixed-resize-debug.apk`

SHA256：

```text
FC6FD0B25B6969C756541F1E93F2A1D515C9F140AD19116725F45D39495D0CF3
```

证据根目录：`debug/FLYME-20261012-RESIZE/`。

- `baseline-1-*`、`opaque-1-*`、`no-glass-1-*`、`direct-1-*`、`config-1-*`、`reverse-1-*`：诊断录像与帧结果。
- `fixed-{1,2,3}-*`：最终连续六轮原始录像、前后任务状态、有效切换摘要及逐帧检测结果。
- `fixed-scene-review.png`：双机场景关键帧；各轮 `bottom-frames.png` 保留底部复核序列。
- `scene-test-red.txt`、`scene-home-test-green.txt`：实际渲染测试的红／绿记录。
- `cold-start-*`：冷启动录像及 `startup-review.png`。
- `record-transition.ps1`、`enter-mini.ps1`、`inspect-transition.ps1`、`check-frames.py`：复现与检查脚本。

未读取正文数据库、未输入／保存测试内容、未卸载或清数据。只移除了本轮自行创建的设备临时录屏／截图／UI层级文件，本地录像保留。未提交／推送Git，未上传Dify知识库。

## 用户最后复核

请用平时操作方式，在两机把「未完成」从小窗返回全屏，检查：① 没有白横条；② 潮汐树不突然重新取景；③ 两颗底部按钮不短暂重复；④ 小窗内原已确认的底部位置仍正确。

该反馈用于本轮动态窗口修复签收，不扩展为第11条全部性能或正式发行验收。
