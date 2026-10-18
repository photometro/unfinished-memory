# 未完成（Unfinished）项目上下文

## 2026-10-08 日常真机 Debug 安装（用户授权）

用户确认魅族21现有 `com.unfinished.app` 是旧测试版，测试数据可覆盖；核对后它与当前 Debug APK 使用相同的本机 Android Debug 证书，因此采用 `adb install -r` 原位更新，未卸载、未清数据。魅族18s原先没有该包，已首次安装。两机分别立即重复覆盖安装均成功，未遇到 `INSTALL_FAILED_USER_RESTRICTED`；M21 `firstInstallTime` 保持原值，M18s后续安装沿用同一包和签名。

日常真机 UI／功能调试入口为 `tools/build-apk.ps1 -Mode debug -Install -TargetSerial <adb序列号>`：包名 `com.unfinished.app`、稳定本机 Debug 签名、`adb install -r`，不卸载、不清数据；多设备同时在线必须指定目标。此前第9条协议自动化仍使用隔离 `protocoltest` 包，第11条目视/性能自动化按各自隔离流程；不要用 Macrobenchmark 作为每次手动调试的安装入口。正式签名配置与密钥保管尚未设置，正式发行时另行处理。若 Flyme 将来仍主动要求系统级确认，不尝试绕过系统策略。

## 2026-10-07 第9条收尾与第10条授权边界

NATIVE-20261007-9-03仅复核9-02已完成的实施验收、产物和文档，不新增代码或设备操作。第9条R1保持verified；完整实施证据见9-02报告，收尾回执见9-03报告。用户随后明确批准实施第10条，因此当前只执行#10/R1；#11及以后仍须逐项取得用户批准。Study18/Study16与#10产出的生产UI、冲突/墓碑及密钥恢复规格仍为候选，用户确认前不作为下游既定基线。真实前后端往返仅用隔离测试资料与本地loopback后端/临时PostgreSQL，不访问旧版App个人库或部署到ECS。

## 2026-10-07 第10条候选实施与验证（NATIVE-20261007-10-01）

#10/R1的七项交付已形成：Study18/16页面状态蓝图与保护清单、首页/主动遇见/过去搜索/性能预算候选、同步和密钥恢复候选规格，以及 Room→Spring→临时PostgreSQL→pull→解密的真实端到端验证。Android JVM与魅族21隔离套件通过；端到端同步在魅族21及模拟器各通过1次。服务端边界验证覆盖幂等重试、等版本冲突、更高版本、过期版本、删除墓碑、禁止复活及稳定分页。数据库仅含合成验证记录且不含测试明文。完整记录见 `docs/implementation-reports/NATIVE-20261007-10-01.md` 和 `debug/NATIVE-20261007-10-01/evidence.md`。

这些结果只证明本地隔离技术候选可行。首页 A/B/C、主动入口、Past 搜索范围、性能门槛、冲突/墓碑产品语义和恢复凭据形式仍待用户审核；#10在清单中保持待复核，#11及以后没有开始。魅族上的正式 `com.unfinished.app` 包与数据未覆盖或读取。

## 2026-10-07 #9 恢复实施（NATIVE-20261007-9-02）

承接下方旧Run，现行本地数据库为Room v4；1→2→3→4／2→3→4和3→4均走明确无损迁移。输入采集模式与会话当前模式分开，旧行默认unknown；停止后不重复采集，旧盲回忆不能因后来的归档/照片曝光或重复页面flush变成POST答案。草稿/回忆读取失败不解锁操作，成功重试保留真实输入。删除墓碑不允许archive复活。

本Run已完成android-runtime验收：3项JVM，断网模拟器和魅族各26正常断言，另行seed/force-stop/recover两项均通过；独立数据/UI复核通过。手机安装并启动的是「未完成·协议测试」，旧版未覆盖，不读取个人库。报告为docs/implementation-reports/NATIVE-20261007-9-02.md。

不以此更新代表新UI/生产评分/后端已完成。下一阶段是#10开发规格与早期前后端数据贯通；#10～17仍needs-decision，未自动实施。下方2026-10-06为前Run历史事实，不能将其v3描述当作现行schema。

## 2026-10-06 #9 协议底座更新（NATIVE-20261006-9-01）

新版业务规则以 IMPROVEMENTS #9 R1 和 ADR 0009 为准。下文三卡视觉是旧原生历史，不是新版原型目标。本轮仅数据接线，不将旧三卡换回正式设计，也不改纸船／洋流／图片。

- Room v3：DailyDecision、曝光、分阶段加密 RecallSnapshot、逐图授权；无独立 ShoreEvent。RecallSession 承担可恢复 EncounterSession。
- 自动／主动共享每日一次成功新事件；scope＋localDateKey 唯一，zone 是快照。AUTO NO 缓存但不阻止有额度的主动。固定到期先扫描、独立排队、保留 dueAt。
- 沉寂14天；自动池4、主动池3；原内容30天、Cue7天、默认暂缓7天。21天不是保底。未配置生产评估器不回退旧随机，显示 MODEL_NOT_CONFIGURED。
- 只有采用线索进入 CueBank；照片逐资产默认不授权。过去只用明确标题／日期，legacy标题和曝光未知保持unknown。
- STOP_RECALL持久停止；DIRECT_REVEAL无回答前置，不造空快照。读取请求不曝光；前台、顶层可见、正文节点实际入视口并过可见帧后才写曝光＋冷却。照片解码／完整显示后记录媒体。
- Compose消费Repository StateFlow；页面不再拥有临时业务真相；回应跳过仍提交完成。
- 新事实暂限本地底座；旧 Outbox 哈希类型未变，未声称服务器已同步新表。正式评分／视觉／后端贯通仍是后续条目。
- 验证用 com.unfinished.app.protocoltest，APK manifest必须再次核对；不得用旧 tools/build-apk.ps1 -Install 覆盖用户旧版。

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
