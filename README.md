# 未完成 · Unfinished

> 一款 local-first 的个人数字记忆应用：把当下留下来，让记忆在合适的时候回来。

“未完成”不是把生活整理成一张张规整的清单，而是保留那些尚未想清楚的片段。用户可以先写下一句话，再决定要不要保留本地提取的候选线索；当记忆重新出现时，应用通过逐层提示让用户选择是否回看原文、暂缓或退出。

## 目标

- 降低记录门槛：允许一段话、一个念头或一张图片先被留下。
- 保留原始语境：原文、线索、回忆和后来产生的回应彼此分层，不互相覆盖。
- 让记忆自然回来：通过可配置的浮现策略，在合适的时间重新遇见过去。
- 把隐私放在默认位置：本地优先、可离线使用，联网能力由用户主动开启。

## 核心体验

首页以潮岸环境为视觉背景，底部提供“过去”和“留下”两个主要入口。回忆、记录和设置等流程由原生 Compose 页面承载；首页与内页的视觉衔接仍在完善中。

| 入口 | 作用 | 主要流程 |
| --- | --- | --- |
| **留下** | 保存一段文字或照片 | 输入 → 草稿保存 → 照片整理 → 封存 |
| **遇见** | 重新接近一段被封存的记忆 | Cue → Recall → Reveal → Reflection（完整自动浮现参数仍待标定） |
| **过去** | 浏览已经保存的内容 | 记忆列表 → 记忆详情 |

### 回忆线索

当前“留下”流程会从原文生成少量本地候选词，用户明确采纳后才保存为 Cue。可配置 AI 提供方的代码仍独立存在，尚未接入这条原生记录流程；AI 不负责判断记忆价值或决定浮现顺序。

### 记忆浮现

模型结构已拆分为资格检查、发生决策、带权候选选择和回忆交互。V3 的发生与候选参数仍未标定，生产随机评估器尚未配置；交互设计保留先看 Cue，再由用户选择查看原文、暂缓或离开的路径。

## 隐私与数据边界

- 首次启动自动创建匿名资料库，不要求登录。
- 记忆优先保存在设备本地，断网时仍可完成记录、封存、浮现和回忆。
- 部分本地字段使用 Android Keystore 管理的密钥与 AES-GCM 加密；照片原件和预览保存在应用私有目录并分别加密。
- 可配置 AI 提供方只有在调用时才会收到相应输入；当前原生“留下”主流程使用本地候选词提取。
- 服务端同步协议接收加密记录及必要元数据，不解密记忆正文；普通记录流程与完整多设备同步、媒体上传和恢复链路仍在实施中。
- 账号绑定、管理后台和云端部署保留接口边界，默认不参与核心体验。

## 当前状态

当前客户端版本为 `0.4.0`，包名为 `com.unfinished.app`。

- ✅ 原生 Compose 首页及“过去”“留下”入口
- ✅ 留下流程的草稿保存、照片私有副本、顺序管理与封存
- ✅ Room 本地数据层与数据库迁移；敏感字段和本地媒体采用加密存储
- ✅ Spring Boot 服务端已有匿名资料库、设备令牌和密文记录同步接口
- 🚧 V3 随机发生评估器未配置，参数标定与长期模拟尚未完成
- 🚧 新同步镜像/Outbox 尚未完整接入普通记录业务与端到端多设备恢复；媒体对象存储接口尚非实际云文件存储
- 🚧 AI 提供方配置代码尚未接入当前原生记录主流程；账号体系、生产部署和正式签名尚未完成

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

日常真机调试使用普通 Debug 包（固定 `com.unfinished.app` 包名和本机 Android Debug 签名），按 ADB 序列号明确指定设备：

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices -l
.\tools\build-apk.ps1 -Mode debug -Install -TargetSerial <设备序列号>
```

此流程只执行 `adb install -r` 覆盖更新，不卸载应用、不清除数据；安装前会核对 APK 包名和签名。两台或更多设备同时在线时必须传 `-TargetSerial`，避免误装到另一台设备。魅族上需先在系统设置中允许 USB 安装；后续相同包名、相同 Debug 签名的更新沿用已授权的 ADB 安装通道。若 Flyme 仍弹出系统级确认，那是设备策略，ADB 参数不能安全绕过。

正式签名配置与密钥保管尚未在此调试流程中设置；发行时再单独接入，不要把 Debug 包当作发行包。`native11benchmark` 等隔离包只用于 Macrobenchmark/自动化验证，不作为日常 UI 调试入口；性能基准部署与 Debug 覆盖安装是两条不同流程。

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

当前进度以实际源码和 `IMPROVEMENTS.md` 为准；部分设计与验收文档保留历史过程记录。

## 设计文档

- [动效与触摸反馈规范](docs/MOTION-TOKENS.md)
- [可复用设计规则](docs/REUSE-RULES.md)
- [产品上下文](docs/CONTEXT.md)
- [API 草案](docs/api/openapi.yaml)
- [数据库 ER 图](docs/database/ER.md)
- [D2 图标设计](design/icon-options/icon-three-cards-balanced.svg)

## 开源说明

项目仍在持续完善中。当前仓库暂未声明具体开源许可证；如需将代码用于其他项目，请先联系作者确认授权范围。
