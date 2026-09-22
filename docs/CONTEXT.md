# 未完成（Unfinished）项目上下文

## 目标

构建一款隐私优先的个人数字记忆系统。用户先留下记忆，记忆进入沉寂；未来由规则与随机机制重新浮现，用户先通过线索回忆，再查看原始内容，并可记录当下感受。

## V1 已确认边界

- 首页只有“留下、遇见、过去”三张交错卡片；设置是唯一独立页面。
- 不设置底部 Tab 栏。
- App 不显示注册或登录页面。
- 首次启动自动创建匿名资料库身份（vaultId/deviceId/deviceToken）。
- Room 是客户端首选数据源；断网可以完成核心闭环。
- Original 封存后不可编辑，删除采用软删除和同步墓碑。
- AI 仅用于生成可编辑的 Cue 建议，不做情绪分析、记忆评分或浮现排序。
- AI 配置复用“观己”自定义模式：Base URL、Model、API Key、连接测试、保存/编辑。
- V1 不开发管理后台；只预留账号绑定、备份、同步、远程配置和管理扩展接口。

## 领域词汇

| 词汇 | 含义 |
|---|---|
| Memory | 一段记忆的聚合身份和生命周期 |
| Original | 封存时形成的不可变原文快照 |
| Cue | 用于逐层唤回记忆的线索，来源为手动或 AI 建议 |
| ResurfacePolicy | 用户为一段记忆选择的浮现规则 |
| ResurfaceEvent | 某一次实际产生的浮现事件 |
| RecallSession | 用户一次从线索到揭晓的回忆过程 |
| Reflection | 用户查看原文后记录的当下感受 |
| Vault | 不需要账号登录的本地/同步资料库 |

## 状态边界

```text
MemoryState: DRAFT / SEALED / ARCHIVED / DELETED
ResurfaceEventState: AVAILABLE / OPENED / DEFERRED / EXPIRED / COMPLETED
RecallSessionState: CREATED / CUEING / ANSWERED / REVEALED / COMPLETED / ABANDONED
```

“沉寂”和“可浮现”是根据时间、策略、冷却期计算得到的派生条件，不与 MemoryState 混用。

## 数据边界

- 本地 Original、Cue、Recall、Reflection 使用 Keystore AES-GCM 做字段级加密后写入 Room；元数据保留用于索引和状态机。
- 同步前对正文内容加密；服务端只保存密文、版本、时间和同步游标。
- API Key 只进入 Android Keystore，不进入 Room、日志、备份或导出文件。
- AI 请求从 App 直接发送到用户配置的 OpenAI Compatible 提供方。

## 未来扩展接口

```text
AuthGateway
AccountBindingGateway
RecoveryGateway
SyncGateway
BackupProvider
RemoteConfigProvider
AdminExtension
SettingsFeatureRegistry
```

以上接口在 V1 只建立契约，不提供可见的登录或管理后台入口。
