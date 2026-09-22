# 未完成 Android 工程

此目录用于 Kotlin + Jetpack Compose 正式工程。

首批模块：

- `feature-home`
- `feature-leave`
- `feature-encounter`
- `feature-recall`
- `feature-past`
- `feature-settings`
- `domain`
- `data-local`
- `data-remote`
- `sync`
- `design-system`

当前 V1 已包含：

- Compose 三卡首页（紧密层叠、80dp 滑动阈值、触觉反馈、同色功能页）
- Room v2 本地领域表、Keystore AES-GCM 敏感字段加密、软删除与 Outbox
- 留下/遇见/过去/设置四个任务面板（顶层仍只有首页与设置）
- 自定义 OpenAI Compatible AI、默认 Cue 建议、可编辑/删除/新增和本地降级
- 草稿自动保存、图片附件记录、三种浮现策略、逐层 Cue、离线揭晓和 App 锁基础能力
- `SyncGateway`、备份接口和匿名 bootstrap 客户端边界

Debug/Release 构建通过；当前 APK 版本为 `0.4.0`。

构建脚本从 `JAVA_HOME`、`ANDROID_HOME` 和 `GRADLE_USER_HOME` 读取本机工具链路径，不把开发机绝对路径写入项目。

Gradle Wrapper 复用了 `guanji-app/android` 的 Gradle 8.14.3 配置，但没有复制观己的 Capacitor 模块、包名或业务代码。
