# 未完成 · Unfinished

> 一款 local-first 的个人数字记忆应用：把当下留下来，让记忆在合适的时候回来。

“未完成”不是把生活整理成一张张规整的清单，而是保留那些尚未想清楚的片段。你可以先写下一句话，之后再由 AI 提供可编辑的线索；当某段记忆重新出现时，应用会通过逐层提示帮助你回到当时的语境，而不是直接把答案推到面前。

## 目标

- 降低记录门槛：允许一段话、一个念头或一张图片先被留下。
- 保留原始语境：原文、线索、回忆和后来产生的回应彼此分层，不互相覆盖。
- 让记忆自然回来：通过可配置的浮现策略，在合适的时间重新遇见过去。
- 把隐私放在默认位置：本地优先、可离线使用，联网能力由用户主动开启。

## 核心体验

首页由三张交错卡片组成。卡片既是视觉主体，也是三个任务流程的入口；左右滑动只切换卡组，不代表删除或拒绝。

| 入口 | 作用 | 主要流程 |
| --- | --- | --- |
| **留下** | 捕捉还没有整理好的内容 | 输入 → 自动保存 → AI 线索建议 → 选择浮现方式 → 封存 |
| **遇见** | 重新接近一段被封存的记忆 | 线索 → Recall → Reveal → Reflection |
| **过去** | 浏览已经发生过的内容 | 搜索 → 时间线 → 记忆详情 |

### AI 线索

AI 只负责从用户输入中提取中性的、可编辑的回忆线索，不做情绪判断、记忆评分或浮现排序。应用支持 OpenAI Compatible 自定义提供方；没有配置 AI 时，会自动回退到本地关键词提取。

### 记忆浮现

浮现可以按用户选择的策略执行，例如随机、间隔或指定时间。遇见时先展示线索，再由用户决定是否继续揭晓原文、推迟或写下现在的回应。

## 隐私与数据边界

- 首次启动自动创建匿名资料库，不要求登录。
- 记忆优先保存在设备本地，断网时仍可完成记录、封存、浮现和回忆。
- 敏感字段使用 Android Keystore 与 AES-GCM 加密保存。
- 只有在用户配置 AI 提供方并发起请求时，相关输入才会发送到该提供方。
- 可选同步服务只接收加密记录、版本和墓碑信息，不解密 Original、Cue、Recall 或 Reflection。
- 账号绑定、管理后台和云端部署保留接口边界，默认不参与核心体验。

## 当前状态

当前客户端版本为 `0.4.0`，包名为 `com.unfinished.app`。

- ✅ Android Compose 三卡首页与 D2 卡片图标
- ✅ 卡片跟手、飞出、补位、同色功能页转场和减弱动效
- ✅ 留下、遇见、过去、设置四个任务面板
- ✅ 草稿自动保存、图片附件、AI 默认线索与本地降级
- ✅ Room 本地数据层、Keystore 加密、匿名同步与备份接口
- 🚧 账号登录、管理后台和 ECS 生产部署：已预留接口，尚未作为默认功能开放

## 技术结构

- **Android**：Kotlin、Jetpack Compose、Room、Android Keystore
- **AI**：OpenAI Compatible HTTP API，可配置 Base URL、Model 和 API Key
- **服务端**：Java 21、Spring Boot、PostgreSQL、Flyway
- **原型**：HTML、CSS、JavaScript，用于验证信息架构、卡片交互和动效基准
- **部署**：Docker Compose 模板，支持后续迁移到 ECS 或其他主机

## 快速开始

### 运行 Android

准备 JDK 21、Android SDK，并设置 `JAVA_HOME` 与 `ANDROID_HOME`。

```powershell
cd android
.\gradlew.bat :app:assembleDebug --offline
```

从项目根目录使用构建脚本：

```powershell
.\tools\build-apk.ps1 -Mode debug
```

### 查看交互原型

```powershell
cd prototype
powershell -ExecutionPolicy Bypass -File .\start.ps1
```

也可以直接打开 `prototype/index.html`。

### 运行服务端

服务端需要 PostgreSQL 和 Java 21：

```powershell
cd server
mvn -DskipTests package
```

### 使用 Docker Compose

```powershell
Copy-Item deploy/.env.example deploy/.env
# 编辑 deploy/.env，填写随机数据库密码
docker compose --env-file deploy/.env -f deploy/docker-compose.yml up -d --build
```

不要把真实密码、API Key、证书或本机配置提交到仓库。

## 项目结构

```text
android/          Android Compose 客户端
server/           Spring Boot 匿名同步服务
prototype/        可交互 UI 原型
design/           图标与视觉探索稿
docs/             API、ADR、数据库和设计规则
plans/            动效与实施计划
deploy/           Docker Compose 部署模板
IMPROVEMENTS.md   版本改进与验收记录
```

## 设计文档

- [动效与触摸反馈规范](docs/MOTION-TOKENS.md)
- [可复用设计规则](docs/REUSE-RULES.md)
- [产品上下文](docs/CONTEXT.md)
- [API 草案](docs/api/openapi.yaml)
- [数据库 ER 图](docs/database/ER.md)
- [D2 图标设计](design/icon-options/icon-three-cards-balanced.svg)

## 开源说明

项目仍在持续完善中。当前仓库暂未声明具体开源许可证；如需将代码用于其他项目，请先联系作者确认授权范围。
