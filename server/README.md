# 未完成 Server 工程

此目录用于 Java + Spring Boot 后端。

当前已完成 V1 服务端：

- `GET /api/health`
- `POST /api/device/bootstrap`（匿名资料库、设备和令牌哈希持久化）
- `POST /api/sync/push`、`GET /api/sync/pull`（密文增量同步与游标）
- `POST /api/backup`、`GET /api/backup/{snapshotId}`（加密备份快照）
- `POST /api/media/upload-ticket`、`DELETE /api/media`（私有图片票据和删除标记）

bootstrap 将令牌以 SHA-256 哈希保存，响应只返回一次明文令牌。服务端不解密 Original、Cue、Recall 或 Reflection。

Flyway `V1__baseline.sql` 建立匿名身份表，`V2__sync_and_backup.sql` 建立密文记录、同步游标、备份快照和图片对象表。

依赖版本已对齐观己后端基线：Java 21、Spring Boot 3.4.5、Flyway 10.20.1、PostgreSQL JDBC 和 Actuator。当前不复制观己的包名、数据库表或业务代码。

V1 负责匿名设备 bootstrap、加密记录同步、备份版本、图片上传票据和健康检查；不负责传统账号登录、记忆内容后台管理或日常 Original 揭晓。
图片票据当前返回适配器 URL；生产部署时将 `MediaController` 的 URL builder 替换为阿里云 OSS 私有 Bucket 签名适配器。

开发机可用本地 Maven 缓存执行 `javac --release 21` 源码校验；若 Docker Hub 镜像代理可用，再执行 `docker compose up -d --build` 完成集成启动。

推荐模块：

- `device`
- `sync`
- `backup`
- `media`
- `health`
- `common`
