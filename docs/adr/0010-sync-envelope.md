# ADR 0010 — 同步载荷、稳定游标与版本化密钥包（候选）

状态：第10条 R1 的实现候选，等待协议评审；不自动成为#11–#17生产基线。范围仅覆盖最薄文本往返与隔离恢复验证。完整媒体、产品化恢复界面和跨设备冲突 UI 仍属后续任务。

## 约束

- Room 是离线业务真相。`scopeId` 保持本地稳定，不能用服务端新建的 `vaultId` 覆盖它；这会改变#9按本地日／时区执行的唯一额度与既有事件归属。
- 服务端只保存不透明密文、必要标识、版本、墓碑与同步游标。服务器不得收到正文散列、恢复秘密、API Key 或设备令牌明文以外的字段；设备令牌只用于 Authorization 请求头。
- 原有 `LocalCipher` 的 Keystore 密文格式继续可读，不在本Run重写用户库。LocalCipher密文不能直接跨设备解开。
- 同一请求的重试必须使用相同 operationId、版本和已封装密文；成功 ACK 只覆盖服务器明确确认的操作。

## 载荷候选 V1

1. 一个 vault 使用随机生成的 256-bit 内容密钥；每条同步记录用 AES-256-GCM 加密 UTF-8 的版本化 JSON 文档。
2. JSON 密文头包含 `envelopeVersion`、`keyId`、`algorithm`、nonce 与密文；AAD 绑定远端 `vaultId`、`entityType`、`entityId`、实体版本和载荷版本。篡改身份、版本或内容时解密失败。
3. `contentHash` 是密文封装字节的 SHA-256，不是明文摘要。它用于传输一致性检查，不能替代 GCM 鉴别标签。
4. App 在设备上用 Android Keystore 中不可导出的 AES-GCM 密钥保存内容密钥设备包；设备令牌用同一设备级加密边界加密存于本地偏好，不生成本地伪 token。
5. 恢复包为 Base64URL 编码的紧凑二进制封装，包含版本、vaultId、keyId、随机盐、nonce 与 GCM 密文。隔离验证以独立随机 256-bit 恢复秘密进行 HKDF-SHA-256 派生，再包住内容密钥；每个新实例必须从包解出同一 key 并解密同一文本。
6. 当前 `RecoveryCodeStore` 生成的四组四位数字及 SHA-256 本地校验不是可接受的恢复密钥，不能直接接入恢复包。人可抄录的恢复凭据格式、校验提示、泄露威胁模型及轮换策略保留为待确认项；#10不展示凭据 UI。
7. 旧 LocalCipher 内容改写必须作为另一个可恢复迁移：保留旧密文、逐记录阶段标记、可重入/断点续做、哈希验证；不清库、不改设备主密钥、不在本次触碰个人库。

隔离验证只证明信封算法在不同密钥实例和独立 Room 文件之间可恢复；它不证明真实用户恢复流程或旧数据迁移已经产品化。

## 记录与 ACK 候选

| 字段 | 用途 |
|---|---|
| `operationId` | UUID Outbox 操作幂等 ID；服务端按 vault 唯一保存操作回执 |
| `recordId` | 对 `(entityType, entityId)` 稳定的记录 ID，不随重试改变 |
| `entityType/entityId` | 远端密文快照定位键 |
| `version` | 同一实体的单调本地版本；不可用墙上时间代替 |
| `ciphertext/contentHash` | 版本化 AES-GCM 封装及密文散列 |
| `deletedAt` | 墓碑；保留为可同步事实 |

Push 在单个服务端事务内返回逐操作 `operationId + outcome`。`APPLIED`、相同请求重试的 `DUPLICATE`、已被更新版本取代的 `STALE` 是可 ACK 状态；`CONFLICT` 与 `TOMBSTONE_REJECTED` 不得标成已同步。网络期间新增的 Outbox 行不属于捕获批次，不能被本次结果标记。

幂等回执对相同 operationId 保存请求哈希和结果；相同 ID／不同请求体返回冲突，不覆盖已接受数据。批内任何数据库异常使本批事务回滚。

## 冲突／墓碑候选

- Incoming version 小于当前版本：返回 `STALE`，不改密文。
- 版本相同且密文封装完全相同：`DUPLICATE`。
- 版本相同但密文／墓碑不同：`CONFLICT`，不做时间戳 LWW。
- 更高版本可以替换普通记录；已有墓碑不得被非墓碑载荷复活。恢复删除记录须另有明确产品意图和审计动作，#10不定义入口。
- 该规则是安全的单实体原型策略，不等于最终多设备合并协议；冲突显示与保留多版本策略需用户确认后在后续同步契约中冻结。

## 游标与事务候选

- 不使用 `updated_at`、服务器当前时间或时间戳加记录 ID 作游标。
- Push 先锁定目标 vault 行，再于同一事务更新当前密文快照、写入幂等回执及 append-only `sync_changes` 序号；同一 vault 的写事务按锁顺序提交，避免先分配序号但后提交导致游标越过未提交行。
- Pull 仅按 `(vaultId, changeSequence > cursor) ORDER BY changeSequence LIMIT pageSize` 读取；`nextCursor` 等于返回页最后一条实际序号，无记录时保持原 cursor。`hasMore` 查询下一条是否存在。
- 客户端在同一 Room 事务中应用一页密文镜像并保存其 nextCursor；重复页幂等，应用异常时 cursor 不前进。`sync_mirror` 是第10条最小入站适配，不冒充完整领域合并或 UI。

第10条的 `sync_mirror` 当前只证明完整密文镜像页的原子落库、游标推进与测试文本解密；它不自动把任意远端记录合并进 #9 的多个领域表，也不定义跨设备 UI 冲突流程。

## 本地身份与离线边界

- `LocalVaultIdentity` 仅负责#9本地 scope 和本地 device 标签；首次联网时 `POST /api/device/bootstrap` 返回服务端身份与服务器随机令牌，客户端持久化远端身份，令牌本地加密。
- 网络未完成 bootstrap 时，记忆仍可离线保存；同步请求停止并明确“尚未连接”，不会把 UUID 当 Bearer token 发送。
- 一个客户端把 Android Keystore 保护的内容密钥重包成与远端 vault 绑定的恢复包之前，不能声称另一个设备可以恢复；#10只使用隔离测试秘密，不上传恢复包。

## 验收测试候选

1. AES-GCM 文本可跨独立恢复秘密封装/解封；错误秘密、篡改字节、错误 vaultId/entity/version 均失败。
2. 旧 Room v4 到新 schema 的迁移保留每行现有值和原密文；新 Outbox 版本不重复；故障注入后可以重试。
3. bootstrap 返回真实 server identity；无网络时不生成 fake auth token。令牌不出现在 Room、同步密文、普通日志或导出。
4. Outbox 只标记批次 ACK 的 operationIds；过程中新增的行保持 pending；失败也只改变本批实际发送行。
5. 真实 PostgreSQL 下验证多页稳定游标、同一操作重复上传、版本冲突和墓碑保护。
6. 独立 Room 数据库拉取密文后，用隔离恢复包解密回同一虚构文本；PostgreSQL只出现密文，不出现该文本及其明文 SHA-256。

## 未确认且不得默认成为生产规则

- 恢复凭据的用户可读格式与重置／丢失语义。
- 多设备并发的用户处理方式和 tombstone restore policy。
- 页面基线：A/B/C首页构图、主动遇见入口、过去搜索范围、性能阈值见 `docs/native-v1-baseline-candidate.md`。
- 真实用户既有密文的迁移批次、恢复、回滚与迁移前备份操作。
