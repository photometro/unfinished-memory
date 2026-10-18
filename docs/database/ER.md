# 未完成 V1 数据模型

## Room v5 结构（#9 业务底座 + #10 同步适配候选）

此节为现行本地结构；v4 是 #9/R1 已验收的不可变业务底座，v5 仅追加加密 outbox、远端密文镜像和分页游标，不改 #9 业务规则。下文更早规划中的 localVersion 等字段不代表已实现。实际 schema 由 `android/app/schemas/com.unfinished.app.data.UnfinishedDatabase/5.json` 导出；旧 schema 保留用于升级夹具。

| 表 | 新事实／职责 | 关键约束 |
|---|---|---|
| memories | userTitle/titleSource，曝光历史可信性，lastOriginalExposureAt/cooldownUntil/deferUntil | 原title和密文保留；未知不推断blind |
| resurface_policies | randomAllowed、planId、dateSource、planCompletedAt、再次随机许可 | 旧fake日期保留unknown；v1缺政策从已有mode回填，不补日期 |
| resurface_events | source、dueAt、登记时刻、activeScope、fixedPlanId、业务日期/zone、legacyState | activeScope和fixedPlanId各自唯一；无ShoreEvent |
| recall_sessions | 来源/模式、inputRecallMode、停止/锁定、revealActionId、outcome、legacyState | 输入模式随真实内容变更，而非页面恢复更新；请求与成功可见分开，既有ID保留 |
| daily_decisions | 自动结果与共享额度分开，规则版本和时间快照 | PRIMARY KEY(scopeId,localDateKey)，zone不在去重键 |
| exposures | SHORE/CUE/ORIGINAL/ORIGINAL_MEDIA、动作、范围、session可空、日期/zone | PK(actionId,kind,contentScope)，重复不更新冷却 |
| recall_snapshots | 加密内容、当时已可见Cue、回忆模式 | 稳定阶段动作ID，插入ABORT，不改写旧快照 |
| media_assets / memory_cues | 每图cueAllowed=false；Cue授权来源USER_EXPLICIT | 旧Cue原值保留，但未知授权不自动成为CueBank |
| vault_settings | 本地随机开关 | 每scope一份，跨进程恢复 |
| sync_outbox | 固定 operationId、recordId、实体版本、密文、密文 hash、逐项 ACK 状态 | 网络批次捕获后只确认本批ID；失败重试复用同一请求数据 |
| sync_mirror | 当前远端密文快照、版本、墓碑、最后 changeSequence | 不保存解密正文；同版本分歧使整页和游标一起回滚 |
| sync_state | 稳定服务端序号游标 | 一页镜像写入与 cursor 更新在同一 Room 事务 |

所有多表业务命令使用同一Room事务；唯一键是最终防线。写入请求／读原文／加载失败不产生曝光。真正可见回执同时写曝光、会话事实和冷却；固定计划只有正式Reveal成功才完成，照片Cue不是Reveal。

创建期间可以改DRAFT原文，seal原子标记不可变；SEALED/ARCHIVED/DELETED禁止saveDraft。迁移不 destructive fallback，不从旧revealedAt补曝光，不强制降库。

## 本地 Room 表

### memories

- `id`：本地 UUID
- `title`：可选标题
- `occurredAt`、`timePrecision`
- `state`：DRAFT / SEALED / ARCHIVED / DELETED
- `sealedAt`
- `createdAt`、`updatedAt`
- `localVersion`、`remoteVersion`
- `syncState`
- `deletedAt`

### memory_originals

- `memoryId`
- `encryptedContent`
- `contentHash`
- `sealedAt`

Original 只允许在封存事务中创建，不提供更新 DAO。

### memory_cues

- `id`、`memoryId`
- `type`：TIME / PLACE / PERSON / KEYWORD / IMAGE
- `value`
- `source`：MANUAL / AI_SUGGESTED
- `confirmed`
- `orderIndex`

### resurface_policies

- `memoryId`
- `mode`：QUIET / SERENDIPITY / FIXED_DATE
- `minDormantDays`
- `fixedDate`
- `notificationEnabled`

### resurface_events

- `id`、`memoryId`
- `state`
- `scheduledAt`、`openedAt`、`deferredUntil`、`completedAt`
- `selectionSeed`

### recall_sessions

- `id`、`memoryId`、`resurfaceEventId`
- `state`
- `clarity`：CLEAR / PARTIAL / VAGUE / NONE
- `recallContent`
- `startedAt`、`answeredAt`、`revealedAt`、`completedAt`

### reflections

- `id`、`memoryId`、`recallSessionId`
- `content`
- `createdAt`

### media_assets

- `id`、`memoryId`
- `localUri`、`remoteObjectId`
- `sha256`
- `uploadState`
- `deletedAt`

### sync_operations

- `id`
- `entityType`、`entityId`
- `operationType`
- `payloadHash`
- `attempts`
- `state`：PENDING / SYNCED / FAILED
- `createdAt`、`lastAttemptAt`

### vault_metadata

- `vaultId`
- `deviceId`
- `createdAt`
- `lastSyncCursor`
- `recoveryState`

## 服务端表

- `vaults`：匿名资料库
- `devices`：设备公钥和撤销状态
- `device_tokens`：令牌哈希及过期时间
- `encrypted_records`：加密实体、版本、墓碑和时间
- `media_objects`：OSS 对象映射
- `sync_cursors`：设备同步游标
- `sync_vault_locks`：同一 vault 的 push 写事务串行化，防止序号提交顺序越过未提交变更
- `sync_operation_receipts`：按 vault 与 operationId 固定请求 hash、逐操作结果和变更序号
- `sync_changes`：追加式变更日志，Pull 用 `change_sequence` 而非更新时间分页
- `backup_snapshots`：加密备份版本

服务端不建立 `users`、手机号、密码和可阅读的 Original 字段。
