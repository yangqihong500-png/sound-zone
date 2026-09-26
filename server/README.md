# 同频 SoundZone · 后端

固定使用 Spring Boot 3.3、Java 17、Spring Data JPA、MySQL 8，保留按模块组织的 entity / repository / service / controller / dto 分层。

## 启动

需要 JDK 17、Maven 和 MySQL 8。项目根目录的 `docker compose up -d` 可启动本地数据库。

**先处理表结构**：全新数据库执行 `sql/schema.sql`；已有数据库先备份，再按 [迁移说明](sql/migrations/README.md) 执行增量 SQL。默认 `ddl-auto=validate`，应用不会自动建表或修改业务库。

在 `server/` 内执行：

```bash
# 独立 Demo 默认允许游客，并幂等初始化常驻演示用户与域
mvn spring-boot:run
# 构建
mvn clean package
# 独立 H2 测试库执行行为测试
mvn test
```

接口前缀 `http://localhost:8080/api`。Audius 默认开启，搜索空关键词返回热门歌曲，关键词搜索返回相关歌曲；外部服务不可用时回退到已落库曲目。历史示例曲目只是元数据，无真实音源。演示成员也遵循心跳超时规则，不会永久保持在线。

游客入口由 `SOUNDZONE_GUEST_ENABLED` 控制，默认开启。公开部署可关闭游客并接入自己的账号体系；`HostIdentityProvider` 仅作为未来宿主合作的可选适配。数据源通过 `SPRING_DATASOURCE_URL / USERNAME / PASSWORD` 覆盖；沿用本地 Docker 配置只是开发默认值。

## 配置与模块

| 配置 | 用途 |
|---|---|
| `SOUNDZONE_IMAGE_DIRECTORY` | 持久图片目录，默认相对工作目录的 `./data/images` |
| `SOUNDZONE_AUDIO_DIRECTORY` | 自有演示音频目录，默认相对工作目录的 `./data/audio` |
| `SOUNDZONE_ADMIN_KEY` | 指标与授权导出的运营凭证；为空时关闭 |
| `SOUNDZONE_ALLOWED_ORIGINS` | HTTP / WebSocket 允许的来源；默认仅 localhost 与 127.0.0.1 |
| `SOUNDZONE_GUEST_ENABLED` | 独立游客入口，默认 `true` |
| `SOUNDZONE_DEMO_DATA_ENABLED` | 常驻演示数据，默认 `true`；正式环境可设为 `false` |
| `SOUNDZONE_MAX_TRACK_DURATION_SECONDS` | 单曲最长时长，默认 `600` 秒；搜索、建域、点歌和播放均校验 |
| `SOUNDZONE_AUDIUS_ENABLED` | Audius 曲库开关，默认 `true` |
| `SOUNDZONE_AUDIUS_API_KEY` | Audius 开发者 API Key；未配置时只读请求使用 `app_name` |
| `SOUNDZONE_AUDIUS_BEARER_TOKEN` | 仅服务端搜索请求可用的 Bearer Token；不要交给前端 |
| `soundzone.music.streams` | 曲目 ID 到已获授权 HTTPS 音源的映射 |
| `soundzone.music.local-catalog` | 带授权引用的本地小曲库配置 |
| `soundzone.active-window-minutes` | 首页近期活跃窗口，默认 30 分钟 |
| `soundzone.presence-grace-seconds` | 成员失联宽限，默认 90 秒 |

`auth` 处理会话与准入；`zone / queue` 处理 FIFO、成员与服务端播放时钟；`realtime` 处理事务提交后推送；`moment` 处理文件校验、即时分享与授权；`feedback / user` 处理幂等互动和个人内容；`activity` 汇总最小必要业务事件。演示初始化器使用 `demo:*` 稳定身份写入 8 个用户和 4 个公开域，重复启动不会重复插入；只对带 `demo_resident` 标记的域保持在线并循环歌单。

详细规则、完整 API 要点、指标口径与曲库接入边界见 [实施说明](../docs/06-implementation.md)。普通接口身份来自 Bearer 会话，不信任 body/query 中的 userId。图片读取同样检查身份与成员资格。

### 授权小曲库示例

音频可使用已获在线播放许可的 HTTPS 地址，也可导入你自己有权使用的文件；两种方式都必须填写授权引用，且默认不得超过 10 分钟。缺少 `license-reference`、时长超限、使用未知标签、同时配置两种音源或本地文件不存在时，应用会拒绝启动。

```yaml
soundzone:
  music:
    local-catalog:
      - key: indie-demo-001
        title: 示例曲目
        artist: 独立音乐人
        stream-url: https://media.example.com/indie-demo-001.mp3
        cover-url: https://media.example.com/indie-demo-001.jpg
        duration-sec: 218
        tags: ["英语", "电子", "舒缓"]
        attribution: 独立音乐人 · 经授权用于 SoundZone Demo
        license-reference: contract:SZ-DEMO-2026-001
```

本地文件方案不依赖 Audius。先把至少 3 首 MP3／M4A 文件复制到 `server/data/audio/`，再把曲目逐条配置为 `audio-file`；文件名只能位于该目录根级，支持 `mp3 / m4a / aac / ogg / wav`。例如：

```yaml
soundzone:
  music:
    local-catalog:
      - key: my-demo-001
        title: 我的演示音乐
        artist: 演示作者
        audio-file: my-demo-001.mp3
        duration-sec: 180
        tags: ["纯音乐", "舒缓"]
        attribution: 自有演示音频
        license-reference: owner:self
```

登记后重启后端，曲目会幂等进入曲库，前端可像 Audius 曲目一样建域和点歌；登记达到 3 首后，常驻演示域会优先使用本地曲库。后端以同源静态媒体提供文件并支持浏览器 Range 请求；不要把无授权的商业歌曲放入公开部署。

若启动提示 `Port 8080 was already in use`，先访问 `http://127.0.0.1:8080/api/actuator/health`：返回 `{"status":"UP"}` 说明后端已经运行，不要重复启动。若提示 `Communications link failure`，先在项目根目录执行 `docker compose up -d mysql` 并等待容器状态变为 healthy。

## MySQL 验收

同一组测试支持在已迁移的**独立** MySQL 库运行：

```bash
SOUNDZONE_TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/soundzone_codex_test_local?serverTimezone=Asia/Shanghai' \
SOUNDZONE_TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver \
SOUNDZONE_TEST_DB_USER=soundzone \
SOUNDZONE_TEST_DB_PASSWORD='<测试库密码>' \
SOUNDZONE_TEST_DDL=validate \
SOUNDZONE_TEST_DIALECT=org.hibernate.dialect.MySQLDialect mvn test
```

测试会清理所连接库中的测试业务数据，只允许 URL 中带 `soundzone_test` 或 `soundzone_codex_test_` 的库。不要指向已有业务库。迁移保留历史字段和行，回滚优先回退应用，禁止用删表或清库替代迁移。
