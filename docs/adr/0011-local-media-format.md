# ADR-0011：第一阶段本地图片与加密封装

- 状态：#10 / R3 工程规格提案；适用于 #12 实施前的兼容预检。
- 范围：完成 #10 清单第 9 项的媒体／加密规格与后端关闭边界核验。本文不代表图片导入、缩略图、数据库迁移或新版 UI 已实现，也不启动 #11。

## 已确认的产品边界

- 每段记忆最多 20 张静态图片；保存私有原图副本，并生成独立显示缩略图。原图不能被压缩图覆盖或静默替换。
- App 的核心记录、过去、遇见和回忆不依赖业务后端、远端身份或 AI。首版不提供本地导出／备份；卸载、清除 App 数据或丢失设备后的恢复不作保证。
- 照片用于 Cue 或发送给 AI 都需要独立的逐图明确授权；默认禁止。未获授权的照片不能成为 Cue，也不能发送给 AI。
- 后端未接入时，不生成远端身份，不发送同步请求，不把本地排队误记为已同步。

## 媒体输入与格式建议

1. 仅通过 Android Photo Picker 选择图片，优先使用多选契约并把选择上限设为剩余名额（总数不超过 20）；不申请读取整库照片权限。收到 URI 后立即在 IO 线程完成复制，数据库不长期保存来源 URI 或临时授权。
2. 静态 JPEG、PNG、WebP作为基础可读格式；HEIF/HEIC、AVIF仅在当前系统 `ImageDecoder` 声明支持且实际解码成功时接受。格式/MIME声明只是提示，仍须解码验证。动画 GIF/WebP、视频、RAW/DNG及损坏／不支持媒体明确拒绝，不将首帧伪装为静态原图。
3. 原图按字节原样保留（含原文件元数据），只对独立预览图做方向归一和缩放。EXIF 等潜在位置元数据始终随原图留在 App 私有密文中，不复制到可查询明文列，不自动显示、分享、用于 Cue 或发送给 AI。
4. **建议值（不是 V3 参数，也不是产品隐性承诺）**：单图原文件上限 64 MiB；单段记忆原图累计上限 512 MiB；解码输入尺寸上限 200 MP（用 `Long` 计算像素数）；预览图最长边 1024 px，WebP lossy quality 90，透明通道保留。导入前后检查私有内部存储可用空间，需能容纳当前加密文件并额外保留 `max(64 MiB, 本次数据量的 10%)`；任一门槛失败都显式报错，不降质覆盖原图。数值须在 #12 的 API 33/较新系统及高像素样本上压力验证后才冻结。

Android 系统／OEM 对部分媒体格式的解码能力可能不同，因此 HEIF/AVIF 是能力探测式支持，不承诺所有机型均可读。图片数量上限、私有副本和不静默降质来自已确认产品规则；字节、像素、余量和缩略图值是这次提出的工程护栏，不冒称来自行业统一标准。

### 2026-10-13 #12 工程标定记录（非产品规则变更）

- 上方64MiB保留为#10历史提案。实际API35／192MiB堆的64MiB合法PNG导入，两次在Keystore GCM解密缓冲扩容阶段OOM；尚未到像素解码，不是“大图已缩采样就能保证安全”。失败留稿／清理本次孤儿文件，不损坏已引用资产。
- Android Keystore GCM在认证完成前缓冲解密输出，16KiB输入分块不等于常量内存解密。[AOSP实现](https://android.googlesource.com/platform/frameworks/base/+/78a3fdfbed02ccfaaf917fb1dc4c5bb37f26496b/keystore/java/android/security/keystore2/AndroidKeyStoreAuthenticatedAESCipherSpi.java)与本轮堆栈吻合。
- 新导入单图候选上限调整为32MiB；API35真实32MiB输入、完整认证、独立预览及累计预算边界测试已通过。64MiB现明确提前拒绝，不压缩或替换原图，不改20张、UFMI v1、密钥别名与授权。
- 兼容读取不跟随新导入上限下降：已有UFMI v1原图仍按64MiB格式读取上限识别。超过新导入护栏的历史全量读取／校验先要求可用堆至少`6 × 原图字节数 + 16MiB`；资源不足只报告“当前内存不足，文件和预览已保留”，不更改READY或删除路径。该值是保守预算而非通用峰值保证，OOM仍捕获为明确读取失败；独立预览不依赖原图全量解密。
- 512MiB累计预算、200MP、1024px／WebP90和空间保留值暂维持；两台真机护栏、现有密文读取兼容与完整回归仍须记录，未完成不得称已冻结。测试细节和最终身份以NATIVE-20261013-12-01报告为准；不使用largeHeap、反射系统缓冲或明文临时文件绕过认证。

## 私有存储与加密封装

- 原图密文与独立预览密文存放于 `Context.noBackupFilesDir/media/v1/` 下的随机 ID 文件名；不使用公开共享目录、相册 URI、可清理 cache 或可预测的正文／标题文件名。Room 只保存稳定 ID、相对文件标识、受支持 MIME、尺寸、密文长度／摘要、授权和本地状态等必要元数据，不保留源 URI、原始 EXIF 字段或明文内容摘要。
- 每个表示（`ORIGINAL`、`PREVIEW`）单独用 Android Keystore 中不可导出的 AES-256 密钥，通过 `AES/GCM/NoPadding` 加密；每个文件生成独立随机 96-bit nonce，认证标签 128-bit。建议独立别名 `unfinished.media.aes.v1`，密钥不落 Room、文件、日志或同步载荷。
- 二进制封装固定为：`magic[4]="UFMI" | version:u8=1 | role:u8 | nonceLength:u8=12 | nonce[12] | ciphertext || GCM-tag[16]`。认证附加数据为带长度前缀的规范 UTF-8 元组 `magic, version, role, localVaultId, memoryId, mediaAssetId`；解密必须校验版本、角色、关联 ID 和 GCM 标签，禁止把另一张图／另一种表示的文件挪用成有效资产。
- 校验摘要只对完整密文封装计算 SHA-256，不存未加密原图 SHA-256，避免产生可比对的明文指纹。GCM 标签负责认证；摘要用于本地文件一致性诊断，不替代认证。
- 使用同目录 `.part` 文件写入并完成 AEAD 收尾后，原子重命名为最终密文文件；只有文件验证通过才在 Room 事务中引用。任一步失败清理本次临时／孤儿密文，不能留下“封存成功”状态。跨文件系统与 Room 的操作用补偿清理，不能声称是单一原子事务。
- 当前 Android Manifest 已 `allowBackup=false`；图片仍放在 `noBackupFilesDir`，使媒体不进入 Auto Backup／设备迁移备份。密钥不做首版可导出备份；卸载／清数据后的数据丢失符合当前“不做本地备份”范围，不能宣传可恢复。

## 后端关闭时的边界与核验口径

- 启动：`MainActivity` 只创建本地 `MemoryRepository`；默认构造通过 `VaultIdentityStore.getOrCreate()` 取得本地命名空间，不调用 `VaultBootstrapClient`、`SyncGateway` 或 `OutboxSyncCoordinator`。无业务后端／无远端凭据不阻止启动。
- 保存：Room 本地事务和 Keystore 加密可在无远端凭据时完成。现有模型可能写入 `sync_operations` 的 `PENDING` 记录；这是待接入队列，不是上传完成。没有显式同步执行器／服务端 ACK 时不得将其改成 `SYNCED`。
- 预留接口：同步客户端继续作为显式边界存在，但本地首阶段不得自动 bootstrap、自动 flush、要求登录、等待远端确认或展示虚假的同步成功。可选 AI 是单独的外部网络能力，不代表业务后端已接通。
- 本次运行时测试覆盖无远端凭据启动及本地文本保存、关闭并重新打开 Room 后读取、队列保持 `PENDING`。照片当前尚未落成私有文件：`saveMediaAsset()` 仍保存 URI 元数据；其真实导入、密文文件、来源失效、缩略图与 20/21 张边界由 #12 实现和验证，不能由本次文字测试代替。

## V3 与权限协议对照（只核对，不修改）

- #14：每会话系统主动开放式问题最多 1 个、自动追加 Cue 为 0，只在用户主动请求且 `ShouldContinue` 允许时继续；STOP／直接查看不追问。没有 Cue 不取消记忆浮现资格，照片授权不改变资格或抽取。
- #16：首次启动不请求通知权限；只有用户开启提醒时请求，拒绝／撤回不阻断本地流程；通知不包含 Original 正文或照片。照片发送 AI 必须逐图单独授权。
- 以上作为既有业务协议核对项，不在本 ADR 重开算法、UI、AI 或通知产品决策。

## 来源

- [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)：选中媒体的有限访问、ImageOnly、多选及 URI 授权说明。
- [Android supported media formats](https://developer.android.com/media/platform/supported-formats)：JPEG/PNG/WebP/HEIF/AVIF 的平台格式差异与能力边界。
- [ImageDecoder API](https://developer.android.com/reference/android/graphics/ImageDecoder)：类型检测、尺寸信息和后台解码能力。
- [Android app-specific files](https://developer.android.com/training/data-storage/app-specific)：私有内部文件目录及卸载后清除边界。
- [Android Auto Backup](https://developer.android.com/identity/data/autobackup)：`noBackupFilesDir` 排除规则。
