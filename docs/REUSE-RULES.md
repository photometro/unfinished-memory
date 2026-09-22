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

- 职责：首页三卡承担一级导航，只有最前卡响应超过 80dp 的左右滑动，点击进入同色功能页。
- 入口：`android/app/src/main/java/com/unfinished/app/MainActivity.kt` 的 `HomeScreen` / `MemoryCard`。
- 边界：滑动只轮换卡组，不代表删除、拒绝或暂缓；首页不显示常驻 App 名称或 Tab 栏。
- 验证：APK 可构建并通过 `aapt dump badging` 与 `apksigner verify --verbose`。

## R-04：原型动效与无遮罩触摸反馈

- 职责：统一卡片跟手、飞出、补位、功能页转场和按压反馈，保证真机行为与 UI 原型一致。
- 入口：`MainActivity.kt` 的 `MotionTokens`、`rememberReducedMotion`、`noRippleClickable`。
- 边界：触摸反馈只做轻微缩放，不使用默认 Ripple 或黑色半透明遮罩；系统“减少动画”设置优先。
- 验证：真机点击卡片/设置行无黑色遮罩；卡片滑动后飞出并由后卡补位；Debug APK 构建、签名、安装启动成功。
