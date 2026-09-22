# 未完成 · Unfinished

个人数字记忆系统毕设项目。核心体验是用三张交错卡片作为首页主导航：留下、遇见、过去。项目包含 Android Compose 客户端、可选的 Spring Boot 同步服务、原型页面和 D2 卡片图标设计。

## 当前版本

- Android：`0.4.0`
- 包名：`com.unfinished.app`
- 默认不要求登录，记忆优先本地保存
- AI 采用 OpenAI Compatible 自定义提供方，未配置时自动使用本地线索提取
- 服务端、账号登录和管理后台仅保留接口边界，部署不包含在当前真机调试流程中

## 目录

- `android/`：Kotlin + Jetpack Compose 客户端
- `server/`：Spring Boot 匿名同步服务
- `prototype/`：交互原型与静态视觉基准
- `design/`：图标与设计探索稿
- `docs/`：API、设计约束和复用规则
- `plans/`：实施计划
- `deploy/`：Docker Compose 部署模板
- `IMPROVEMENTS.md`：版本改进与验收记录

## Android 构建

请先准备 JDK 21、Android SDK 和可用的 Gradle Wrapper，并设置 `JAVA_HOME` 与 `ANDROID_HOME`。Windows 下：

```powershell
cd android
./gradlew.bat :app:assembleDebug --offline
```

也可以使用项目脚本：

```powershell
./tools/build-apk.ps1 -Mode debug
```

脚本不会写入账号、API Key 或数据库密码；这些配置应通过环境变量或 App 内设置提供。

## 服务端与部署

服务端需要 PostgreSQL。部署前复制 `deploy/.env.example` 为 `deploy/.env`，填写本地或服务器环境变量后再启动 Docker Compose。不要把真实密码、API Key 或证书提交到仓库。

## 设计基准

首页三卡的层级、颜色继承、D2 图标和动效 Token 记录在 `docs/REUSE-RULES.md` 与 `docs/MOTION-TOKENS.md`。本仓库不包含本地真机截图、构建日志或 APK 产物。
