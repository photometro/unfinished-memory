# 未完成 V1 数据模型

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
- `backup_snapshots`：加密备份版本

服务端不建立 `users`、手机号、密码和可阅读的 Original 字段。
