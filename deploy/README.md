# Unfinished V1 部署

1. 在 ECS 上安装 Docker Compose，并将 `.env.example` 复制为 `.env`，设置随机数据库密码。
2. 生产环境将 `nginx.conf` 的 80 端口替换为 HTTPS 证书配置（可接入 certbot 或阿里云 SSL）。
3. 执行 `docker compose up -d --build`，访问 `/api/health` 验证服务。
4. PostgreSQL 数据卷名为 `postgres-data`；上线后使用 `pg_dump` 做每日加密备份。

如果构建时遇到镜像拉取失败，请检查服务器到 Docker Hub 的网络或配置可用镜像源；开发机的镜像访问结果不能代表 ECS 部署失败。
