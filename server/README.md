# 同频 SoundZone · 后端服务（server/）

基于 **Spring Boot 3.3 + Java 17 + Spring Data JPA** 的后端 Demo，功能模块与 `../docs/01~04` 一一对应。

> **v2 变更（2026-09-24 第二次会议）**：队列改 FIFO（得分公式废除）、新增上传冷却（10 分钟）、标签过滤双模式（BAN/ALLOW）、公开/私密域（密码/邀请码 + 成员制 + 全员退出自动消失）、图片分享绑定上传歌曲（+撤回+半小时图片流）、关注体系、emoji 轻互动、歌品值移除（去游戏化，**域后战报保留**）。

> **技术栈说明（假设标注）**：docs/05 原定后端为 FastAPI（Python），应开发要求改用 Spring Boot 实现。功能模块、接口语义与文档保持一致；docs/05 的技术选型一节待同步修订。

## 快速开始

需要 **JDK 17+** 与 **Maven 3.8+**（或用 IntelliJ IDEA 直接打开本目录，IDE 可自动配置两者）：

```bash
mvn spring-boot:run          # 启动，端口 8080，统一前缀 /api
mvn clean package            # 打包 target/sound-zone-server-0.1.0.jar
```

- 接口前缀：`http://localhost:8080/api`
- H2 控制台：`http://localhost:8080/api/h2-console`（JDBC URL：`jdbc:h2:mem:soundzone`）
- 启动自动注入与前端 mock.js 一致的演示数据（3 个域 + 曲目 + 队列 + 碎片 + 番茄钟配置）

## 项目结构

```
server/
├── pom.xml                         # 依赖：web / validation / data-jpa / h2 / lombok
└── src/main/
    ├── resources/application.yml   # 端口/数据源/JPA 配置（含 PostgreSQL 生产示例）
    └── java/com/soundzone/
        ├── SoundZoneApplication.java
        ├── common/                 # 统一响应 Result、错误码 ResultCode、
        │                           #   业务异常 BizException、全局异常处理
        ├── config/                 # CorsConfig（前端联调）、DataInitializer（种子数据）
        ├── user/                   # 用户：entity / repository / service / controller / dto
        ├── track/                  # 曲目（含曲风标签）
        ├── zone/                   # 域 + 番茄钟时段（ZonePeriod）
        ├── queue/                  # 点歌队列（核心机制）
        ├── moment/                 # 碎片（三元组数据）
        └── feedback/               # 红心/收藏 + 战报（审美反馈）
```

## 数据库表结构

| 表 | 说明 | 关键字段 |
|---|---|---|
| `sz_user` | 用户 | name, taste_score（歌品值） |
| `sz_track` + `sz_track_tags` | 曲目 + 曲风标签集合 | title, artist, source, tags |
| `sz_zone` + `sz_zone_tags` + `sz_zone_banned_tags` | 域 + 风格标签 + 曲风黑名单 | name, scene, host_id, status, listener_count |
| `sz_zone_period` + `sz_zone_period_tags` | 番茄钟时段 + 白名单 | order_index, duration_min, type, allowed_tags |
| `sz_queue_item` | 点歌队列 | track_id, user_id, likes, host_bonus, score, status |
| `sz_moment` | 碎片 | text, image_url, track_id（自动绑定配乐） |
| `sz_feedback_event` | 反馈事件 | track_id, from_user_id, to_user_id, type |

## 接口清单（统一响应 `{code, msg, data}`，code=0 成功）

| 方法 | 路径 | 说明 | 对应文档机制 |
|---|---|---|---|
| GET | `/zones/active?scene=` | 活跃公开域列表（私密域不分发） | docs/02 第 2 步 + D2 |
| POST | `/zones` | 创建域（≥3 首 + 公开/私密 + 过滤双模式 + 番茄钟） | docs/02 第 1 步 + D2/D5 |
| POST | `/zones/{id}/join` | 进入域（私密域密码/邀请码鉴权） | D2 |
| POST | `/zones/{id}/leave` | 退出域（全员退出自动消失） | D2 |
| GET | `/zones/{id}` | 域详情（播放中 + FIFO 队列 + 动态区 1-2 张） | docs/01 三件套 |
| POST | `/zones/{id}/end` | 结束域 | docs/02 第 5 步 |
| POST | `/zones/{zoneId}/queue` | 上传歌曲（冷却→过滤→时段→FIFO 队尾） | D3/D4/D5 |
| GET | `/zones/{zoneId}/cooldown?userId=` | 上传冷却剩余秒数 | D4 |
| POST | `/queue/{itemId}/like` | 点赞（仅信号，不影响顺序） | D3/D7 |
| POST | `/zones/{zoneId}/moments` | 发图片分享（绑定上传歌曲/当前播放） | D6 |
| GET | `/zones/{zoneId}/moments/feed` | 半小时图片流（动态详情页） | D6 |
| DELETE | `/moments/{momentId}?userId=` | 撤回图片（仅本人） | D6 |
| POST | `/zones/{zoneId}/heart` | 互动反馈（COLLECT/LIKE/EMOJI_*，收藏触发微光） | D7 |
| GET | `/zones/{zoneId}/report` | 域后个人战报（保留） | 用户确认保留 |
| POST/DELETE | `/users/{userId}/follow?fromUserId=` | 关注 / 取关 | D7 |
| GET | `/users/{userId}/following` | 我的关注列表 | D7 |
| GET | `/tracks/search?keyword=` | 上传搜曲 | — |
| GET | `/users/{userId}/profile` | 我的页（上传/获赞/分享/关注，无歌品值） | D7 |

### 上传准入流程（v2）

```
上传 → ① 冷却检查（10 分钟内已传 → 3005 拒绝，返回剩余秒数）
     → ② 域级标签过滤（BAN 命中 → 3002 / ALLOW 不含 → 3009）
     → ③ 番茄钟时段白名单（不符合 → 入 PRESET 预存队列）
     → ④ 入队尾（FIFO = createdAt 升序，无排序模型）
```

## 假设标注（文档未明确处的决策）

| # | 假设 | 说明 |
|---|---|---|
| 1 | 后端栈切换为 Spring Boot | docs/05 原定 FastAPI，按开发要求实现 |
| 2 | Demo 数据库用 H2 内存库 | 生产切 PostgreSQL（application.yml 已附示例），实体无需改动 |
| 3 | 听众数 listenerCount 为冗余字段 | 正式版由 WebSocket 在线连接实时统计（docs/05 接入层） |
| 4 | 当前时段按"域创建至今分钟数 % 番茄钟周期"推算 | 正式版由服务端权威时钟统一推进并广播 |
| 5 | 红心归属 = 当前播放条目的点歌人 | 非当前播放曲目反馈返回参数错误 |
| 6 | 歌品值 = 获赞总数/点歌总数 × 100（上限 100） | docs/02 定义"被点赞率"，Demo 简化为聚合比例 |
| 7 | 无登录体系，userId 显式传递 | 正式版从登录态解析 |
| 8 | 场景归一化用静态别名映射表 | 正式版为文本聚类模型（docs/03） |
| 9 | 服务层未拆接口/实现两个文件 | 单一 @Service 类满足分层；如需面向接口可再抽 |
| 10 | 实时推送（WS）未实现 | 当前 REST 轮询语义；WS 接入层属 M1 后续迭代（docs/05） |

## 联调前端

前端 `app/src/api/mock.js` 各函数与上述接口一一对应，把函数体换成 `uni.request({url: 'http://localhost:8080/api' + ...})` 即可完成联调。
