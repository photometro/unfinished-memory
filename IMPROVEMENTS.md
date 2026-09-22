# 未完成 · 改进与实施清单

## 1. V1：本地优先、无登录、匿名加密同步的个人数字记忆系统 🟡 核心实现完成，待部署验收

### 需求

将当前 UI 原型和产品方案落地为一款可运行的 Android 个人数字记忆 App：

- 首页保留“留下、遇见、过去”三张交错卡片。
- 首页与设置页是仅有的两个顶层页面，不设置底部 Tab 栏。
- App 原则上不显示注册、登录页面。
- 第一次启动自动创建匿名资料库身份。
- 记忆、线索、回忆和原文默认本地保存。
- 断网时也可以完成记录、封存、浮现、回忆和揭晓。
- Spring Boot 后端作为毕设 P0，负责匿名设备身份、加密同步、备份和图片管理。
- AI 复用“观己”的自定义提供方模式。
- AI 只生成可编辑的线索建议，不参与情绪判断、记忆评分或浮现排序。
- V1 不开发管理后台，但在代码、接口和设置能力层预留未来扩展。

### 现状

- 当前项目根目录为 `C:/Users/43124/Desktop/test/unfinished`。
- 已完成 Android Compose、Spring Boot、Room 加密数据层、AI 提供方、匿名同步服务和 Docker Compose 工程；部署与真机验收仍待环境条件满足。
- 已建立 `IMPROVEMENTS.md`，复用规则在 `docs/REUSE-RULES.md`。
- 原型已经包含三卡布局、首页安静状态、返回信号、卡片颜色继承和“留下”流程。
- “观己”已有自定义 AI 配置与安全存储规则，可作为配置协议和实现约束的来源。

### 理解与设计（草案）

#### 1. 产品结构

顶层页面只有：

1. 首页：三张卡片作为主导航。
2. 设置：AI、隐私、App 锁、备份和数据服务。

卡片打开的是任务流程，不是三个长期存在的 Tab：

```text
留下：输入 → AI 线索建议 → 用户确认 → 策略 → 封存
遇见：线索 → 回忆 → 清晰程度 → 揭晓 → 感受
过去：搜索 → 详情 → 历史回忆
```

#### 2. AI 方案

复用“观己”的自定义模式：

- Base URL
- Model
- API Key
- 连接测试
- 保存/编辑配置

实现规则：

- 使用 OpenAI Compatible `/chat/completions` 接口。
- API Key 仅保存到 Android Keystore，不写入 Room、日志或备份。
- 进入线索页后自动生成建议。
- 建议必须允许用户修改、删除和新增。
- AI 未配置、超时或失败时，自动降级到手动线索输入。
- AI 请求直接由 App 发往用户配置的提供方，不经过“未完成”业务服务器。

#### 3. 无登录方案

首次启动时后台生成：

```text
vaultId       本地资料库标识
deviceId      设备标识
encryptionKey 本地加密密钥
deviceToken   匿名后端访问令牌
```

用户不需要看到账号、密码、手机号或登录页。

同时预留未来账号能力，但不在 V1 暴露入口：

- `AccountBindingGateway`
- `AuthGateway`
- `RecoveryGateway`

未来可以通过恢复码、二维码或账号绑定完成设备迁移，而不需要重写本地数据层。

#### 4. 后端定位

后端不负责强制控制原文揭晓，主要负责：

- 匿名设备注册
- 加密数据同步
- 加密备份与恢复
- 图片私有存储
- 版本和同步游标
- 服务健康检查

服务器不保存可直接阅读的 Original、Cue、Recall 和 Reflection 内容，只保存必要的密文、版本号和时间信息。

建议技术栈：

- Android：Kotlin、Jetpack Compose、Room、WorkManager、Retrofit
- 后端：Java、Spring Boot、Spring Security、MyBatis-Plus、Flyway
- 数据库：PostgreSQL
- 图片：阿里云 OSS 私有 Bucket
- 部署：Docker Compose、阿里云 ECS、Nginx、HTTPS

#### 5. 本地领域状态

不要用一个 `status` 字段表示全部状态，拆成三套状态机：

```text
MemoryState
DRAFT / SEALED / ARCHIVED / DELETED

ResurfaceEventState
AVAILABLE / OPENED / DEFERRED / EXPIRED / COMPLETED

RecallSessionState
CREATED / CUEING / ANSWERED / REVEALED / COMPLETED / ABANDONED
```

离线揭晓流程：

```text
查看线索 → 提交回忆 → ANSWERED → 本地解锁 Original → REVEALED
```

#### 6. 管理后台预留

V1 不开发管理后台，也不在 App 中放置不可用的“管理后台”按钮。

仅预留以下接口和能力位：

- `SyncGateway`
- `BackupProvider`
- `RemoteConfigProvider`
- `AdminExtension`
- `SettingsFeatureRegistry`

设置页预留“数据与服务”分组。只有未来能力真正启用后，才显示备份、同步、设备迁移或远程配置项目。

### 复用映射

| 需求元素 | 现有规则/函数/class | 代码位置 | 采用方式 |
|---|---|---|---|
| 三张卡片交错布局 | `slotLayouts`、`renderDeck` | `prototype/app.js:7、98` | 转为 Compose `HomeCardDeck`，保留槽位参数和层级逻辑 |
| 首页安静/返回状态 | `renderState`、`returnSignal` | `prototype/app.js:112、607` | 转为 `HomeUiState.Quiet/Returned` |
| 留下流程 | `leaveStep`、`renderLeaveStep` | `prototype/app.js:36、380` | 转为 `LeaveViewModel` 状态机 |
| 功能页继承卡片颜色 | `.function-panel[data-kind]` | `prototype/styles.css:432` | 转为 Compose 主题 Token |
| 自定义 AI 配置 | AI 提供方配置规则 | `guanji-app/docs/REUSE-RULES.md:322-330` | 复用 Base URL、Model、API Key、连接测试和安全存储规则 |
| AI 请求协议 | `aiEndpoint`、`aiRequest` | `guanji-app/www/js/ai.js:479、547` | 封装为 Kotlin `CueSuggestionProvider` |

### 新增例外

当前“未完成”没有正式生产代码和可直接复用的规则，因此以下模块需要新增：

- Android Compose 页面和设计系统。
- Room 本地领域模型。
- 匿名资料库身份。
- 本地浮现算法。
- 离线回忆与揭晓状态机。
- Spring Boot 匿名同步服务。
- 加密备份和恢复接口。

新增模块的职责仅限于上述范围，不改变首页已确认的交互和视觉规则。

### 实施清单

#### 阶段 A：规格与工程骨架

- [x] **U-00（P0，1天）** 固化 `CONTEXT.md`、领域术语、三套状态机和 V1 范围。
- [x] **U-00（P0，1天）** 建立数据库 ER 图、OpenAPI 初稿和以下 ADR：匿名资料库、离线揭晓、自定义 AI。
- [x] **U-01（P0，1天）** 创建正式目录：`android/`、`server/`、`docs/`、`deploy/`，保留 `prototype/`。
- [x] **U-01（P0，1天）** 建立 Android、Spring Boot 和 Docker Compose 最小可运行骨架。

#### 阶段 B：Android UI 与本地数据

- [x] **U-02（P0，3天）** 将首页三卡迁移为 Compose `HomeCardDeck`（当前为首屏可运行切片，组件抽取与完整槽位参数仍待后续优化）。
- [x] **U-02（P0，1天）** 实现卡片滑动、层级、阈值、触觉反馈、点击打开和卡片颜色继承。
- [x] **U-02（P0，1天）** 实现首页安静态、返回信号和设置入口。
- [x] **U-03（P0，4天）** 建立 Room 表：记忆、Original、Cue、策略、浮现事件、回忆会话、感受、图片和同步队列。
- [x] **U-03（P0，2天）** 建立 `MemoryState`、`ResurfaceEventState`、`RecallSessionState` 三套状态机。
- [x] **U-03（P0，2天）** 完成敏感字段应用层加密、Android Keystore 密钥保存和 App 锁基础能力。

#### 阶段 C：留下流程与 AI

- [x] **U-04（P0，3天）** 实现文字、图片、草稿自动保存、退出恢复和草稿删除。
- [x] **U-05（P0，2天）** 实现自定义 AI 配置、连接测试、保存/编辑和安全存储。
- [x] **U-05（P0，2天）** 实现进入线索页后自动生成 AI 建议。
- [x] **U-05（P0，1天）** 实现 AI 建议编辑、删除、新增和失败降级。
- [x] **U-06（P0，2天）** 实现 QUIET、SERENDIPITY、FIXED_DATE 三种浮现策略。
- [x] **U-06（P0，1天）** 实现封存确认，封存后 Original 不可编辑。

#### 阶段 D：浮现与回忆闭环

- [x] **U-07（P0，3天）** 实现本地候选筛选、沉寂时间、冷却期、随机抽取和“当天无记忆”状态。
- [x] **U-07（P0，1天）** 增加仅开发环境可见的时间推进和浮现触发工具。
- [x] **U-08（P0，4天）** 实现遇见、逐层线索、回忆输入、清晰程度选择和暂缓。
- [x] **U-08（P0，2天）** 实现离线提交回忆、离线揭晓 Original、Reflection 和再次沉寂。
- [x] **U-09（P0，3天）** 实现过去搜索、详情、回忆历史和主动查看原文。

#### 阶段 E：匿名后端与同步

- [x] **U-10（P0，4天）** 实现 `POST /api/device/bootstrap`，生成匿名资料库和设备令牌。
- [x] **U-10（P0，3天）** 完成 PostgreSQL、Flyway、JdbcTemplate 和 OpenAPI 基础配置。
- [x] **U-10（P0，2天）** 预留 `AuthGateway`、`AccountBindingGateway` 和 `RecoveryGateway`，不制作登录 UI。
- [x] **U-11（P0，4天）** 实现 `SyncGateway`、Outbox、增量 push/pull 和同步游标。
- [x] **U-11（P0，3天）** 实现 Original、Cue、Recall、Reflection 的本地/上传前密文边界。
- [x] **U-11（P0，2天）** 实现恢复码、加密备份和删除同步（恢复码本地生成；服务端快照接口已完成）。
- [ ] **U-12（P0，2天）** 实现 OSS 私有图片上传、签名访问、本地缓存和删除。
- [x] **U-12（P0，1天）** 预留 `BackupProvider`、`RemoteConfigProvider`、`AdminExtension` 和 `SettingsFeatureRegistry`。

#### 阶段 F：稳定、部署与毕设材料

- [ ] **U-13（P0，3天）** 完成断网、重试、同步冲突、AI 超时和本地数据损坏处理。
- [ ] **U-14（P0，4天）** 完成状态机单元测试、Room 测试、Compose UI 测试、后端接口测试和越权测试。
- [ ] **U-15（P0，2天）** 使用 Docker Compose 部署到阿里云 ECS，配置 PostgreSQL、OSS、Nginx、HTTPS 和数据库备份。
- [ ] **U-16（P0，3天）** 整理系统架构图、ER 图、接口文档、测试报告、演示视频和论文截图。

#### P1：后续扩展

- [ ] P1：账号绑定和正式登录入口。
- [ ] P1：多设备同时编辑与复杂冲突解决界面。
- [ ] P1：管理后台 Web 界面。
- [ ] P1：WebDAV、推送通知、音频、视频和高级语义搜索。

### 已确认约束

- 账号登录和管理后台均只预留接口，V1 不实现可见入口。
- 不以答辩时间倒推实现；后续按实际测试与部署条件推进。

### 验收要点

- App 首次启动没有登录页，但会自动创建匿名资料库身份。
- 无网络时可以完整完成“留下 → 封存 → 遇见 → 回忆 → 揭晓”。
- AI 已配置时，线索页默认自动出现建议；AI 失败时可以手动完成。
- 封存后的 Original 无法编辑，删除和同步状态正确。
- 服务端不能直接读取上传的 Original、Cue、Recall 和 Reflection 内容。
- 不同 `vaultId` 之间不能访问彼此的数据。
- API Key 不出现在 Room、日志、备份和导出文件中。
- 首页三卡布局、层叠关系、手势和颜色继承与当前 UI 原型一致。
- App 内没有空的登录或管理后台页面，但代码中存在未来扩展接口。
- 项目能够通过 Docker Compose 部署到阿里云 ECS，并完成数据库备份。

## 2. 【Bug】原型与真机首页纵向位置及色彩还原偏差 ✅ 已实施（v0.3.0）

### 需求

按照已经确认的 UI 原型，修正真机首页卡组的位置和颜色表现：

- 三张卡片整体应在页面下部聚拢，不能在真机上显得偏上。
- 保留三张卡片的高度、顺序、交错关系和内部内容布局。
- 卡片颜色应保持原型的淡雅、低饱和视觉，不应出现真机上偏鲜艳的效果。
- 继续保留“过去、遇见、留下”的颜色身份和点击后同色面板规则。

### 现状

- 真机 `HomeScreen` 当前通过固定的 `BoxWithConstraints.height(650.dp)` 和顶部 `padding(top = 144.dp)` 放置卡组，纵向位置没有根据底部安全区域适配。
- 原型卡组使用底部边界约束，真机截图中的前卡相对底部提示显得偏上。
- 原型与 Android 当前基础颜色 Token 基本相同，但 Android 使用纯色 `Page`、`CardDefaults.cardColors` 和 Material 阴影，缺少原型的渐变、噪点和柔和叠加，因此视觉上更鲜艳。

### 理解与设计（草案）

- 以卡组容器为单位进行底部安全区域定位，不分别修改三张卡片的相对位置。
- 以前卡底部与底部手势提示之间的稳定间距作为真机校准依据，具体偏移不写死为单一设备数值。
- 继续使用原型的颜色语义，但降低视觉饱和度、提高明度，并恢复背景渐变、轻微噪点和柔和阴影。
- 禁止引入 Material 动态取色或额外 tonal elevation，避免再次改变原型色彩。

### 实施清单

1. [x] 调整 `HomeScreen` 卡组容器的纵向锚点，使其适配状态栏、导航栏和底部安全区域。
2. [x] 保持 `MemoryCard` 的卡片尺寸、槽位顺序、旋转角度、交错间距和手势逻辑不变。
3. [x] 在目标真机上校准前卡底部、卡组底部与手势提示的间距，并记录截图结果。
4. [x] 对照 `prototype/styles.css` 统一 `Page`、卡片颜色、渐变、噪点和阴影参数。
5. [x] 通过真机截图复核“淡雅配色”和首页上下比例，确认没有影响点击、滑动和面板颜色继承。

### 待确认问题

- 暂无。

### 验收要点

- 三张卡片整体位于首页下部，前卡底部与底部手势提示保持稳定、舒适的间距。
- 卡片之间的层叠顺序、交错关系、尺寸和滑动行为与当前原型一致。
- 真机截图中的过去、遇见、留下卡片呈现淡雅、低饱和效果，与原型观感一致。
- 首页背景具备原型的渐变层次，卡片阴影柔和，不出现过强的 Material 色彩或阴影。
- 点击卡片、左右滑动和打开同色功能面板的行为不回归。

## 记录

- 2026-09-08：根据用户确认，创建“未完成”V1 实施待办；确定无登录、离线揭晓、自定义 AI、后端保留、管理后台暂不开发但预留接口。
- 2026-09-08：开始实施 #1；完成 U-00 规格文档、ER 图、OpenAPI 初稿、3 份 ADR，以及 U-01 正式目录创建。Android/Spring Boot 可运行骨架及后续功能仍待实施和验证。
- 2026-09-08：补充 Server 最小骨架（Spring Boot 配置、`/api/health`、`/api/device/bootstrap`、Dockerfile）；已通过原型 `app.js` 语法检查和 Docker Compose 配置检查。当时主机 Maven/Gradle 命令尚未配置，Android 和后端编译留待工具链迁移后验证。
- 2026-09-08：复用观己 Gradle Wrapper（Gradle 8.14.3）、JDK 21、Android SDK 和本地缓存；建立独立 `com.unfinished.app` Compose 工程。首次离线构建因 Compose 依赖未缓存失败，显式联网补齐依赖后统一 JVM 目标，第二次离线 Debug 构建成功，APK 为 `android/app/build/outputs/apk/debug/app-debug.apk`，SHA-256 为 `2955843DE956CF200DB281B75E387F8A306F2878D6CAD248443639B1AE47E546`。
- 2026-09-08：后端 Docker 构建已启动但因本机镜像代理不可用，未能拉取 Maven/Temurin 基础镜像；未将该环境问题误判为项目编译成功或失败，等待后续切换可用镜像或提供本地 Maven。
- 2026-09-08：补充 Android Compose 首页首屏切片：三张彩色交错卡片、左右拨动轮换、点击打开同色功能面板、设置入口和 112dp 顶部留白；移除首页多余说明句。Debug APK 再次离线构建成功，包名 `com.unfinished.app`、versionName `0.1.0`，SHA-256 为 `CA683E7D62062DFD412DCCF38F2CEDA9E51EB773C5D18481E49A24B55D544393`，签名为 Android Debug v2。
- 2026-09-08：后端补充 PostgreSQL/Flyway 连接配置和 `V1__baseline.sql`，预留 `vaults`、`devices`、`device_tokens` 表；bootstrap 持久化和令牌哈希仍待 U-10 实施。
- 2026-09-08：完成 V1 核心实现：Android Compose 三卡首页与颜色继承、Room v2 本地表和 Keystore AES-GCM、草稿自动保存/图片附件、AI 默认 Cue 建议与降级、三种浮现策略、离线遇见/揭晓/Reflection/推迟、过去搜索与回忆历史、App 锁基础能力、Outbox/HTTP SyncGateway；Android Debug APK 升级至 `0.2.0`，构建成功。
- 2026-09-08：完成服务端匿名 bootstrap 持久化、SHA-256 令牌哈希、密文记录 push/pull、同步游标、备份快照、图片上传票据及扩展接口；新增 Flyway `V2__sync_and_backup.sql` 与 `docs/api/openapi.yaml`。全部 Server Java 源码以本机 Maven 缓存和 `javac --release 21` 编译通过。
- 2026-09-08：验证结果：`aapt dump badging` 显示 `com.unfinished.app` / versionCode 2 / versionName 0.2.0，`apksigner verify` 的 v2 签名通过，`node --check prototype/app.js` 通过，Docker Compose 配置通过。Docker 镜像构建因本机镜像代理不可用无法完成；浏览器/真机回归暂未运行。
- 2026-09-09：补充 `tools/build-apk.ps1 -Install` 真机调试入口：构建后读取 APK SHA-256 证书，与设备已安装包（若存在）比对后再执行 `adb install -r`，安装成功后自动启动并写入本地调试摘要。真机首次安装受系统 USB 安装授权限制，开启开发者选项后继续回归。
- 2026-09-09：按用户反馈以 `prototype/index.html`、`prototype/styles.css`、`prototype/app.js` 为原生 UI 唯一基准，重写 `MainActivity.kt`：恢复 430dp 三卡交错层叠、默认“留下→遇见→过去”牌序与左右拨动轮换、卡片颜色继承的全屏面板、原型文案和分步“留下→AI 线索→浮现方式→确认封存”流程；同步重做遇见、过去、设置页面，移除会与 LazyColumn 冲突的嵌套滚动。AI 本地降级建议标记为 `AI_SUGGESTED`，保证线索页默认展示 AI 建议样式。Debug APK 已通过 `tools/build-apk.ps1 -Mode debug -Install` 安装并启动；真机回归确认首页卡片、滑动、留下/线索/策略/封存、遇见空态、过去页面和设置页面可达且无崩溃，摘要见 `debug/apk-build-summary.json`。
- 2026-09-09：补回草稿恢复页的“删除草稿”低干扰入口并在真机清理调试草稿；最终安装后首页处于干净的安静态。
- 2026-09-09：根据真机截图与 UI 原型对照结果，新增 #2 页面还原待办：卡组改为底部安全区域定位，保留卡片层叠关系，并恢复原型的淡雅配色、渐变、噪点与柔和阴影。
- 2026-09-09：完成 #2（v0.3.0）：`HomeScreen` 改为按可用高度计算卡组底部安全间距，前卡底部距屏幕底部约 86dp；更新 Past/Encounter/Leave 与 Settings 面板色彩、首页/面板渐变和 Material 阴影。APK 构建、安装、原型浏览器回归及真机点击/滑动验证通过，截图见 `debug/device-v2-home-quiet.png`、`debug/device-v2-leave.png`、`debug/device-v2-settings.png`。
- 2026-09-09：用户定版 D2 App 图标方向：复用首页三张纯色卡片和 Past/Encounter/Leave 配色，保持上下重叠关系，只增加左右分离度；正式 Android 图标接入列入 #3。
- 2026-09-09：根据用户反馈，新增 #4 动效还原待办：补齐卡片跟手、后卡抬升、阈值触感、飞出补位、卡片到功能页转场、按钮按压反馈和系统减弱动效适配；执行顺序为 003 → 001 → 002。
- 2026-09-09：补充 #4 点击反馈问题：移除 Compose 默认 Ripple 造成的黑色半透明遮蔽，改用原型定义的轻微缩放/亮度反馈，并保持圆角裁剪、点击语义和无障碍支持。
- 2026-09-09：完成 #3/#4（v0.4.0）：接入 D2 自适应图标与回退资源；新增共享动效 Token、系统减弱动效适配、卡片跟手/后卡联动/弹簧回位/飞出补位、卡片到功能页转场和无遮罩按压反馈。Debug APK 构建、签名、安装启动及真机回归通过；按压区域像素亮度无黑色遮罩回归（卡片区域平均变化 -0.42、设置行 +0.45）。

---

## 3. App 图标：D2 三张纯色记忆卡片 ✅ 已实施（v0.4.0）

### 需求

为“未完成”补充正式 App 图标。图标使用首页三张卡片作为唯一视觉来源，保持首页与启动器入口的一致性。

### 实施结果

- Android `AndroidManifest.xml` 已声明 `android:icon` 与 `android:roundIcon`，并建立 adaptive icon 与回退资源。
- 图标候选已在 `design/icon-options/` 完成多轮原型比较，用户已定版 D2。
- D2 最终稿位于 `design/icon-options/icon-three-cards-balanced.svg`；Android 前景资源已按安全区重新承载，真机启动器检查通过。

### 理解与设计（已确认）

- 三张卡片继续使用首页的 Past、Encounter、Leave 三种底色：`#D0D9DD`、`#E0E7E9`、`#F1EBE3`。
- 保持“后卡 → 中卡 → 前卡”的叠放顺序和上下重叠关系。
- 采用 D2 的水平分离增强：只增加左右露边，不再增加上下距离；卡组整体保持居中，避免形成长条形重心。
- 卡片表面为纯色，不使用边框、缺口、橙色点或内部文字/内容。
- 允许保留非常轻的外部阴影，用于表达首页卡片的层级；阴影不能改变卡片底色和轮廓识别。

### 复用映射

| 需求元素 | 现有规则/函数/class | 代码位置 | 采用方式 |
|---|---|---|---|
| 三张卡片的叠放顺序与层级 | `HomeScreen` 的 `order`、`CardSlot`、`MemoryCard` | `android/app/src/main/java/com/unfinished/app/MainActivity.kt:181-320` | 将最终图标的前/中/后顺序与首页一致，图标仅保留静态几何关系 |
| 首页三种卡片颜色 | `PastColor`、`EncounterColor`、`LeaveColor` | `android/app/src/main/java/com/unfinished/app/MainActivity.kt:87-91` | 直接复用颜色 Token，不新增图标专用色 |
| D2 最终几何稿 | `icon-three-cards-balanced.svg` | `design/icon-options/icon-three-cards-balanced.svg` | 作为 Android adaptive icon 前景/背景制作的视觉基准 |

### 新增例外

暂无。图标实现直接复用首页卡片颜色和层级规则，只新增 Android 启动器资源承载方式。

### 实施清单

1. [x] 将 D2 SVG 转为 Android adaptive icon 所需的前景与背景资源，保留安全区和居中关系。
2. [x] 新增 `mipmap-anydpi-v26/ic_launcher.xml` 及必要的 `mipmap-*` 回退资源。
3. [x] 在 `AndroidManifest.xml` 声明 `android:icon`，不改变应用名称和启动行为。
4. [x] 构建 Debug APK，使用 `aapt dump badging` 确认图标资源已被打包。
5. [x] 在真机启动器中检查不同尺寸/遮罩下的居中、三色层级和可辨识度。

### 待确认问题

- 暂无。D2 已定版；本条只记录接入 Android 的实施工作。

### 验收要点

- 启动器和系统最近任务入口显示“未完成”图标，不再使用空白默认图标。
- 三张卡片保持 Past、Encounter、Leave 三色，前/中/后顺序与首页一致。
- 卡片为无边框纯色面，无缺口、橙色点和内部内容。
- 只存在左右分离增强，上下重叠关系不被进一步拉开；卡组视觉中心稳定。
- Android 自适应图标在常见圆形、方形和圆角方形遮罩下不裁切关键卡面。

---

## 4. 【Bug】真机动画与 UI 原型动效差距较大 ✅ 已实施（v0.4.0）

### 需求

按照已经确认的 UI 原型，恢复真机中的连续、可跟手的卡片与页面动效：

- 首页前卡必须跟随手指移动，后两张卡随滑动进度抬升和缩放，不能只做状态瞬切。
- 超过 82dp 阈值后，前卡应带轻微旋转和淡出飞出，并在完成后平滑补位；未超过阈值时应以短促弹簧回到原位。
- 点击卡片时先出现“选中卡片抬升、拉直、放大”的反馈，再进入对应的同色功能页；设置入口直接进入面板。
- 功能页从底部上升并淡入，返回时反向退出，不能出现与卡片无关的瞬时白屏或页面跳变。
- 主要按钮、次要按钮、文字操作、设置按钮和首页前卡提供轻微按压缩放；尊重系统“移除动画”设置。
- 点击首页卡片或设置页选项时，不应出现黑色半透明遮罩；点击反馈应与原型一致，仅保留轻微缩放或亮度变化。
- 不改变卡片内容、颜色、尺寸、层级、状态机、数据流和导航结构。

### 实施结果

- `MemoryCard` 已使用 `Animatable`、`spring` 与 `tween` 实现前卡跟手、后卡抬升、未过阈值回弹、阈值触感、飞出淡出和 300ms 补位。
- `UnfinishedApp`、`HomeScreen` 与功能面板已建立选中抬升和底部上移/淡入转场，设置入口直接打开面板。
- `MotionTokens` 与 `rememberReducedMotion()` 已统一动效曲线、时长和系统减弱动效分支，详见 `docs/MOTION-TOKENS.md`。
- `MemoryCard`、`SettingRow`、`ChoiceCard`、`PanelHeader`、历史列表和主要操作已关闭默认 Ripple，改为圆角裁剪后的 0.97 轻微缩放反馈。
- 真机按压回归中，卡片区域平均亮度变化为 -0.42、设置行 +0.45，未再出现黑色半透明矩形遮蔽。

### 理解与设计（已确认）

以原型 `prototype/styles.css` 的 `--ease`、卡片拖拽和面板转场为唯一动效基准，拆为三个可执行子计划：

1. **动效基础（003）**：新增共享 `MotionTokens` 和 `rememberReducedMotion()`；统一曲线与时长：原型缓出 `cubic-bezier(0.16, 1, 0.3, 1)`、回弹/补位 `cubic-bezier(0.23, 1, 0.32, 1)`、飞出 `cubic-bezier(0.18, 0.85, 0.28, 1)`，按压 140ms、透明度 180ms、面板 300ms、飞出 280ms；按压目标缩放为 `0.97f`。
2. **首页卡组（001）**：保留现有 `CardSlot` 与 82dp 阈值；拖拽时前卡直接跟手，Y 限制在 `-14..14px`、旋转限制在 `-6..6°`，后卡分别按进度抬升 `16px/8px`、缩放 `0.03f/0.02f`；取消滑动用 `spring(dampingRatio=0.8f, stiffness=300f)`，成功滑动用 280ms 飞出并在完成后只轮换一次牌序，再用 300ms 完成卡组补位。
3. **卡片—功能页衔接（002）**：选中卡片先以 250ms 抬升 `-22dp`、拉直至 `0°`、放大至 `1.015f`；功能面板从自身高度 `102%` 的底部以 300ms 上移并配合 180ms 淡入，返回执行反向过渡；减弱动效时取消位移/旋转/缩放，仅保留必要的透明度和颜色反馈。
4. **点击反馈清理**：关闭卡片、设置行、返回、选择项和历史列表的默认 Ripple（使用 `indication = null` 的统一点击封装）；在点击区域先做圆角裁剪，避免出现方形边界；以原型的 `scale(0.97)` 或卡片 `brightness(0.985)` 作为可控反馈，并继续保留点击语义、触感和无障碍支持。

推荐执行顺序：`003 → 001 → 002`。详细执行记录见 `plans/003-motion-tokens-and-reduced-motion.md`、`plans/001-card-deck-motion-parity.md`、`plans/002-panel-transition-parity.md`。

### 复用映射

| 需求元素 | 现有规则/函数/class | 代码位置 | 采用方式 |
|---|---|---|---|
| 卡片层级、颜色与滑动语义 | R-03；`HomeScreen`、`MemoryCard`、`CardSlot` | `docs/REUSE-RULES.md`；`android/app/src/main/java/com/unfinished/app/MainActivity.kt` | 只补充动效状态和插值，不改变三卡导航、颜色继承、牌序和 82dp 阈值 |
| 原型卡片跟手与飞出 | `.home-card`、`.is-flinging`、`updateGesture`、`finishGesture` | `prototype/styles.css:249-289`；`prototype/app.js:435-490` | 将拖动进度、后卡响应、飞出曲线和补位时序适配为 Compose `Animatable`/`tween`/`spring` |
| 原型卡片选中与面板转场 | `.is-picked`、`.function-panel.is-open`、`openPanel` | `prototype/styles.css:406-451`；`prototype/app.js:492-503` | 复用现有 `FunctionPanel` 和 `PanelScaffold`，在根容器增加统一进入/退出过渡 |
| 卡片颜色继承 | `kind.color` 三段渐变 | `MainActivity.kt` 的 `FunctionPanel` | 面板继续由卡片颜色提供主题，不新增动效专用颜色 |
| 系统减弱动效 | Android 全局 `Settings.Global.ANIMATOR_DURATION_SCALE` | 新增 `rememberReducedMotion()` | 不申请权限；比例为 `0f` 时取消空间位移/旋转/缩放，保留可理解的透明度/颜色反馈 |
| 原型点击反馈 | `.home-card:active`、`.setting-row:active`、按钮 `:active` | `prototype/styles.css:299-301、918-925` | 禁用 Compose 默认黑色 Ripple，复用原型的亮度/缩放反馈，不引入全屏或矩形遮罩 |

### 新增例外

暂无。该待办只在现有 Compose 与原型规则上增加动效状态、共享 Token 和可访问性分支，不改变业务状态机、持久化、后端或页面信息架构。

### 实施清单

1. [x] 新增 `MotionTokens` 与 `rememberReducedMotion()`，并在 `docs/` 补充动效复用说明。
2. [x] 改造 `MainActivity.kt` 的 `MemoryCard`：加入拖拽跟手、后卡联动、取消弹簧、阈值触感、280ms 飞出和 300ms 补位；所有动画限制在 `graphicsLayer`/`alpha`。
3. [x] 改造 `UnfinishedApp`、`HomeScreen` 与 `FunctionPanel` 的根转场：加入选中卡片 250ms 反馈、面板 300ms 上移/180ms 淡入及返回反向过渡；设置入口跳过卡片选中延迟。
4. [x] 将 0.97 按压反馈应用于主要操作、次要操作、文字操作、设置按钮和首页前卡；减弱动效时移除空间缩放。
5. [x] 为 `MemoryCard`、`SettingRow`、`ChoiceCard`、`PanelHeader` 和过去列表等自定义点击面关闭默认 Ripple，统一处理圆角裁剪与原型一致的按压反馈。
6. [x] 使用 Compose 离线构建 Debug APK，并完成原型浏览器与真机回归：慢拖、快滑、未过阈值回弹、连续滑动、卡片点击、设置、返回、按住状态和减弱动效。

### 待确认问题

- 暂无。动效基准、执行顺序和不改变业务/视觉结构的边界均已确认。

### 验收要点

- 前卡拖动全程跟手，后两张卡随进度抬升/缩放，未过阈值时回弹自然且无瞬移。
- 跨过 82dp 后只触发一次触感；前卡按原型飞出曲线在约 280ms 内淡出，牌序只轮换一次，剩余卡片平滑补位。
- 点击卡片先看到对应卡片抬升、拉直、放大，再进入同色功能页；面板从底部上移并淡入，返回无白屏、闪烁或错误颜色。
- 主要操作按下时有克制的 0.97 缩放，松开后恢复；不影响文本输入、滚动和点击命中。
- 按住首页卡片、设置选项和其他自定义点击行时，不再出现黑色半透明矩形或全屏变暗；点击反馈仅表现为原型定义的轻微缩放/亮度变化。
- Android 系统动画比例设为 0 时，空间位移/旋转/缩放被移除，但透明度、颜色反馈和状态变化仍然可理解。
- Debug APK 构建、`aapt dump badging`、`apksigner verify` 及现有首页/流程回归均通过。
