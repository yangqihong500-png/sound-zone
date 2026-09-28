# SoundZone 腾讯云部署

本目录配合仓库根目录的 `docker-compose.prod.yml` 与 GitHub Actions 使用。日常发布入口是推送 `main`；生产服务器不需要手工启动前端、后端或数据库。

## 1. 准备服务器

建议使用腾讯云中国大陆轻量应用服务器或 CVM。轻量服务器优先选择 Docker CE 应用模板，该模板预装 Docker，并默认配置腾讯云 Docker 镜像源，能降低国内服务器拉取基础镜像超时的概率。大陆域名正式开放访问前必须完成 ICP 备案；备案完成前可以先通过受限 IP 或非大陆环境做技术验证。

服务器需要安装 Docker Engine、Docker Compose 插件、`curl` 和 `rsync`。创建专用部署用户，使其只能通过 SSH 密钥登录并可运行 Docker。开放 SSH、80；HTTPS 配置完成后开放 443。不要向公网开放 3306 或 8080。

创建部署目录，例如：

```bash
sudo mkdir -p /opt/sound-zone
sudo chown deploy:deploy /opt/sound-zone
```

首次把仓库同步到服务器后：

```bash
cd /opt/sound-zone
cp .env.production.example .env.production
chmod 600 .env.production
mkdir -p data/images data/audio backups
```

编辑 `.env.production`，替换全部示例密码、管理密钥、域名。真实文件只保留在服务器，不提交 Git。

## 2. 首次验证

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.production -f docker-compose.prod.yml ps
curl --fail http://127.0.0.1/healthz
```

`migrate` 是一次性服务：新库执行完整 `schema.sql`；旧库仅执行尚未记录的 `Vxxx` 脚本。旧库发生迁移前，会在 `backups/` 生成压缩 SQL 备份。迁移失败时，后端不会启动。

## 3. GitHub 自动部署

在 GitHub 仓库设置以下 Actions Secrets：

- `DEPLOY_HOST`：服务器公网 IP 或已解析域名。
- `DEPLOY_PORT`：SSH 端口，通常为 `22`。
- `DEPLOY_USER`：专用部署用户，例如 `deploy`。
- `DEPLOY_PATH`：部署目录，例如 `/opt/sound-zone`。
- `DEPLOY_SSH_KEY`：专用 SSH 私钥。
- `DEPLOY_KNOWN_HOSTS`：预先核验的服务器 host key；不要用未经核验的运行时扫描结果代替。

再创建 Actions Variable：

```text
DEPLOY_ENABLED=true
```

未设置该变量时，GitHub 只运行测试和前端构建，不会连接服务器。Pull Request 也只验证、不部署。

## 4. HTTPS 与域名

当前容器内 Nginx 仅监听 HTTP，便于首次 IP 验证。面向真实用户前，必须完成域名备案并配置 HTTPS。可以在宿主机使用腾讯云证书或 Certbot，把 443 流量转发到本 Compose 的 Web 服务；WebSocket 必须保留 Upgrade/Connection 头并使用 `wss://`。

HTTPS 上线后，把 `.env.production` 的 `PUBLIC_ORIGIN` 改为准确的 `https://你的域名`，然后重新部署。

## 5. 日常发布

`main` 分支中以下路径变化会触发测试和发布：`app/`、`server/`、`deploy/`、生产 Compose 和工作流。只改普通文档不会部署。

```bash
git add <本次文件>
git commit -m "fix: 描述本次修改"
git push origin main
```

不要在生产服务器运行 `docker compose down -v`，该命令会删除 MySQL 命名卷。发布前后可用以下命令观察状态：

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=200 server web migrate
```

在 GitHub 自动发布正式启用前，本机已经可以用一条命令把当前工作区同步到服务器并发布：

```bash
./deploy/deploy-current.sh
```

脚本使用被 Git 忽略的 `.deploy.production.local` 和已经核验的 `.deploy-known-hosts`，不会上传生产环境变量、数据库数据或本机构建缓存。首次构建后的依赖与镜像层会复用，因此日常小改通常只重建发生变化的层。
