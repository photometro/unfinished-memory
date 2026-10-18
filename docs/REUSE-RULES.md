# Unfinished 可复用规则

## R-01：AI 提供方配置与降级

- 职责：统一保存 Base URL、Model、API Key，并在请求失败时回退到本地线索提取。
- 入口：`android/app/src/main/java/com/unfinished/app/ai/AiProvider.kt` 的 `AiProviderConfigStore`、`CueSuggestionProvider`。
- 边界：AI 只能生成可编辑 Cue，不读取本地数据库、不做情绪判断、不参与浮现排序。
- 验证：Android Debug APK 编译成功；未配置或超时时 `suggest()` 返回本地关键词。

## R-02：匿名密文同步

- 职责：以 vault/device/token 识别设备，服务端只接收加密记录、版本和墓碑。
- 入口：Android `SyncGateway` / `OutboxSyncCoordinator`；Server `DeviceAuthService` / `SyncController`。
- 边界：令牌仅保存哈希，服务端不解密 Original、Cue、Recall、Reflection；账号与后台保持接口预留。
- 验证：Spring Java 21 源码使用本机 Maven 缓存编译通过；Docker Compose 配置检查通过。

## R-03：卡片层级与颜色继承

- 适用性：旧三卡原生历史，不作为当前重构UI目标；新视觉以IMPROVEMENTS #6～#8及后续验收切片为准。
- 职责：首页三卡承担一级导航，只有最前卡响应超过 80dp 的左右滑动，点击进入同色功能页。
- 入口：`android/app/src/main/java/com/unfinished/app/MainActivity.kt` 的 `HomeScreen` / `MemoryCard`。
- 边界：滑动只轮换卡组，不代表删除、拒绝或暂缓；首页不显示常驻 App 名称或 Tab 栏。
- 验证：APK 可构建并通过 `aapt dump badging` 与 `apksigner verify --verbose`。

## R-04：原型动效与无遮罩触摸反馈

- 职责：统一卡片跟手、飞出、补位、功能页转场和按压反馈，保证真机行为与 UI 原型一致。
- 入口：`MainActivity.kt` 的 `MotionTokens`、`rememberReducedMotion`、`noRippleClickable`。
- 边界：触摸反馈只做轻微缩放，不使用默认 Ripple 或黑色半透明遮罩；系统“减少动画”设置优先。
- 验证：真机点击卡片/设置行无黑色遮罩；卡片滑动后飞出并由后卡补位；Debug APK 构建、签名、安装启动成功。

## R-05：稳定包名的日常真机调试安装

- 职责：让手动 UI／功能调试复用已安装应用，避免每轮创建新隔离包而触发重复安装确认。
- 入口：`tools/build-apk.ps1 -Mode debug -Install -TargetSerial <adb序列号>`。
- 边界：普通 Debug 保持 `com.unfinished.app` 与本机稳定 Debug 签名；安装前校验包名和签名，只执行 `adb install -r`，不自动卸载或清数据。多台设备在线时必须明确指定序列号。隔离 instrumentation / Macrobenchmark 仍用于自动化验证，不作为日常手动调试安装入口。正式发行签名尚未接入。
- 验证：2026-10-08 魅族21同签名原位更新与魅族18s首次安装后重复更新均成功；两机重复 `install -r` 无 `INSTALL_FAILED_USER_RESTRICTED`，未卸载或清除数据。

## R-05：V1本地业务事实、输入来源与运行隔离

- 入口：MemoryRepository、UnfinishedDatabase（现行v4）、V1Protocol、VisibleReceipt。
- 规则：日决策/额度/事件整事务，scope＋localDateKey唯一、zone只留证；固定到期先扫描、原due不变；模型Unavailable不制造NO。
- 事实：STOP/DIRECT持久化，原文请求不等于曝光；真正可见内容写幂等回执和最长冷却，Cue/媒体逐项显式授权，不认证legacy未知事实。
- 输入：inputRecallMode绑定实际内容采集；重复恢复/flush不能将旧答案变成POST，已锁快照不重复采集。恢复全部成功后才开放操作，保存成功同步恢复标记，重建保留尚未自动落盘的编辑。
- 验证：NATIVE-20261007-9-02，3项JVM；断网API35模拟器和API36魅族各26正常断言＋分阶段重启2项通过，独立数据/UI审查通过。
- 安全：测试须-PprotocolTest且aapt实际核对com.unfinished.app.protocoltest及独立runner；禁止tools/build-apk.ps1 -Install覆盖旧包。测试只用虚构/专属夹具，不清库、不改密钥。
- 边界：本地底座通过不等于生产评分、原生视觉、服务端载荷或个人库迁移通过；上述范围分条验收。
