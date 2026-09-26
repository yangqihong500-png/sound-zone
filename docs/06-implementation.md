# 06 · 已确认方案的实施与验收

2026-09-25 用户确认按审阅方案推进，待确认项采用建议。技术栈仍为 Spring Boot 3.3 / Java 17 / JPA / MySQL 8 / uni-app Vue3；未引入 Redis、消息队列或新的业务框架。

## 本轮确定的规则

- FIFO：上传时间升序，同一时间以队列 ID 升序；初始歌单按用户点选顺序入队。
- 单曲最长 10 分钟。曲库搜索隐藏超长曲目，建域和域内点歌由后端再次拒绝；限制上线前已入队的超长条目在播放推进时标记为 REMOVED 并自动切到下一首。可用 `SOUNDZONE_MAX_TRACK_DURATION_SECONDS` 调整部署值。
- 建域至少三首不同的有效曲目，初始曲目也校验标签。初始批量入队作为创建例外，成功后域主进入十分钟冷却。
- 建域先选择 BAN／ALLOW 模式，再从五类主题标签中选择同一组过滤标签；两种模式都至少选择一个。BAN 任一标签命中即拒绝；ALLOW 任一标签命中即允许。初始歌单中与规则冲突的歌曲须在提交前移除或调整规则。目录由后端统一提供。
- 新建域的 ALLOW 标签同步作为正向展示标签；BAN 标签仅标为禁止标签，不用于正向推荐分类。已有域保留原有过滤规则，编辑域信息时不修改过滤模式和标签。
- 图片绑定具体 queueItemId，且该条目必须属于当前用户、当前域。上传后十分钟内可补图，每次上传最多一张；撤回后本次上传不再重传。图片上传成功后立即展示，不改变歌曲的 FIFO。
- 主界面和动态详情均展示三十分钟内、未撤回且未被历史标记为 REJECTED 的图片；主界面最多两张。旧 PENDING 图片无需再审核即可展示，旧 REJECTED 图片继续隐藏。
- 我的域 = 本人创建的域（含已结束历史）；收藏保存在同频本地关系表，保留后续宿主同步适配空间。
- 图片表情采用 HEART / LAUGH / LIKE 三类，每人每图一个当前表情，可替换或取消；无评论区。
- 首页活跃窗口三十分钟，要求公开、存在成员、正在播放、有近期上传或图片行为；可配置。
- 前端每二十秒心跳，服务端九十秒无心跳视为离开；主动退出即时生效。旧成员首次扫描给予一个完整宽限期。
- 浏览域内动态详情和用户子页时维持共听；离开域页面栈时退出。队列空时不自动补歌；全员退出后结束。
- 不开放手动结束域，不扩建战报；保留旧事件和历史 DTO。AI DJ、域歌单沉淀、番茄钟完整机制仍属后续阶段。
- 新建番茄钟配置暂不开放，旧配置和 PRESET 数据保留，预存歌曲不擅自转成普通待播项。
- Demo 默认幂等写入 8 个演示用户和 4 个公开常驻域（自习／健身／旅行／深夜），优先使用已落库 Audius 曲目；常驻域维持预置成员并循环歌单。该行为只作用于 `demo_resident` 域，可用 `SOUNDZONE_DEMO_DATA_ENABLED=false` 关闭，普通域生命周期不变。

## 已落地的结构

- `auth`：随机 Bearer 会话（库内只存令牌摘要）、宿主身份验证适配接口、当前用户解析、PBKDF2 私密密码摘要、独立运营凭证。
- `zone / queue`：域行锁、并发冷却、稳定 FIFO、权威时钟、自然播完推进、空闲恢复、成员心跳清理与域消散。
- `realtime`：Spring WebSocket，在事务提交后通知已准入成员；收藏微光仅定向上传者。客户端首条消息传凭证，URL 不含会话令牌。
- `moment`：真实 multipart 上传、本地持久存储（可替换）、图片重编码去除元数据、大小与像素限制、即时展示、归属校验、撤回与单选 emoji。
- `feedback / user`：幂等收藏／点赞／关注；我的域、上传、关注、收藏及个人图片查询；真实举报记录。
- `activity`：必要业务事件、基于播放心跳的收听秒数、每日指标；不采集令牌、密码、图片内容或设备标识。
- 前端沿用既有页面与组件；新增会话、播放器、域连接服务及可复用的“我的内容”页。上传、关注、撤回等均不再信任请求中的用户 ID。

## 接入边界（必须如实验收）

### 身份

独立 App 默认 `soundzone.guest-enabled=true`，每个浏览器获得独立游客会话，不再共同使用固定用户。公开部署可用 `SOUNDZONE_GUEST_ENABLED=false` 关闭入口并接入正式账号体系。原 `HostIdentityProvider` 与前端桥保留为未来合作适配器，不再是产品运行前提；任何模式都不信任客户端自报 subject 或 userId。

### 音源与播放器

曲库元数据以本地 `Track` 为稳定映射，`MusicProvider` 输出 STREAM / HOST / UNAVAILABLE：

- Audius 默认开启：空关键词取热门歌曲、非空关键词搜索，公开可播放且未加访问门槛的结果落入 `sz_track`；60 秒短缓存减少重复请求。
- Audius 播放地址由官方 `/v1/tracks/{id}/stream` 生成，前端使用 uni-app 音频组件播放。可配置公开 API Key；服务端 Bearer Token 不下发给客户端。
- `soundzone.music.local-catalog` 导入已获授权的小曲库，可选择 HTTPS 音频或 `soundzone.audio-directory` 下的自有文件，并强制合法标签和授权引用；`soundzone.music.streams` 继续兼容旧的“曲目 ID → HTTPS 音源”映射。
- HOST/TME 曲目带 externalId 时仍可调用 `globalThis.SoundZoneHost.player`，但该分支只是未来可选扩展。
- 未配置音源的 MOCK 曲目会明确显示“尚未接入可播放音源”，不会用无关声音冒充音乐。
- H5 音频按服务端 startedAt、请求中点时钟偏差对齐，并明确显示连接、首播授权、播放和重试状态。浏览器要求首播手势时，“开始同步播放”仅完成授权并对齐此刻，不提供暂停或切歌；网络失败会重建音频上下文。

真实 Audius 搜索、曲目落库和 MP3 流已完成端到端冒烟。腾讯音乐没有接入，不调用逆向接口；若未来获得正式合作，只在 Provider 边界新增适配。公开运营前仍需按具体曲目、地域和商业场景复核授权范围。

### 图片即时展示与存储

默认图片存储于 `server/data/images`（可用 `SOUNDZONE_IMAGE_DIRECTORY` 指定持久卷）。图片不作为公共静态文件开放，读取接口校验登录态与成员资格；撤回后拒绝读取。

新上传的图片在文件校验、重编码和归属校验成功后立即进入动态流，不再等待人工审核。审核适配器、待审核列表与审核接口已移除。数据库中的旧状态字段暂保留，避免丢失历史数据；历史 PENDING 图片可展示，但未被自动改写为 APPROVED，因此不会仅因本次变更进入训练导出。历史 REJECTED 图片继续隐藏。

## API 要点

所有普通接口继续返回 `{code,msg,data}`。错误附带合理 HTTP 状态；冷却错误 `data={remainSeconds,cooldownMinutes}`。身份放 `Authorization: Bearer <token>`。

| 操作 | 接口 |
|---|---|
| 登录配置／独立游客／可选宿主登录 | GET `/sessions/config`；POST `/sessions/guest`、`/sessions/host` |
| 当前用户 | GET `/sessions/me` |
| 加入／退出／心跳 | POST `/zones/{id}/join`、`/leave`、`/heartbeat` |
| 域详情／修改主题信息 | GET / PUT `/zones/{id}` |
| 域主获取邀请凭证 | GET `/zones/{id}/invite` |
| 上传／冷却／点赞状态 | POST `/zones/{id}/queue`；GET `/zones/{id}/cooldown`；PUT `/queue/{itemId}/like` |
| 收藏状态 | PUT `/zones/{id}/collection`，`{itemId,active}` |
| 图片 | POST multipart `/zones/{id}/moments`，file、queueItemId、trainingConsent、可选 text |
| 动态／撤回／表情 | GET `/zones/{id}/moments/feed`；DELETE `/moments/{id}`；PUT `/moments/{id}/reaction` |
| 举报 | POST `/zones/{id}/reports`，`{reason}` |
| 个人列表 | GET `/users/me/zones`、`/uploads`、`/following`、`/collections`、`/moments` |
| 标签目录／曲库／播放源 | GET `/tracks/tags`、`/tracks/search?keyword=`、`/tracks/{id}/playback?zoneId=` |

运营接口统一校验 `X-Admin-Key`：

- GET `/internal/metrics?date=YYYY-MM-DD`。
- POST `/internal/training/exports`：显式生成授权图片清单，保存批次与图片关联，不自动执行训练。
- GET `/internal/training/exports/{batch}/revocations`：查询已撤回／失去授权／历史状态失效的条目。
- GET `/internal/training/images/{id}`：每次读取再次检查授权和撤回状态。

训练消费者必须在每次训练前同步剔除清单；已经复制到外部的数据不可能仅通过本服务软删除自动从外部消失。当前仓库未接入外部训练系统。

## 指标口径

- 活跃用户：当天发生必要业务事件的去重用户。
- 收听秒数：客户端报告实际播放，且条目与服务端当前播放一致时，累加两次心跳之间不超过三十秒的时长；不把断网间隔计为收听。
- 上传参与率：当天普通上传用户数 / 活跃用户数；建域初始三首不计普通上传。
- 图片分享率：当天普通上传条目中已附图条目数 / 当天普通上传数。
- 收藏率：当天开始播放且当天产生新增收藏的播放条目数 / 当天开始播放条目数。
- 次日留存：当天活跃用户中次日仍活跃的比例；次日尚未结束返回 null，分母为零也返回 null。

## 迁移与回滚

见 `server/sql/migrations/README.md`。V003 只把新图片的数据库默认状态设为 APPROVED；V004 增加默认关闭的常驻域标记并兼容旧队列状态列。既有业务库不自动改表、重启或清库；生产启用前按迁移说明备份并切换，应用默认 `ddl-auto=validate`。

## 验证记录

- 后端行为测试覆盖核心业务、单组标签建域、图片即时展示、常驻域生命周期、Audius／本地授权音源解析，以及超长歌曲在搜索、建域、点歌和历史队列中的限制。
- 同一套 12 项测试已在 V002 后的独立 MySQL 8 测试库通过（`ddl-auto=validate`）。
- 全量建表脚本重复执行、V001 迁移重复执行均通过；旧版 13 张表的样本行和旧字段保持不变，新增字段使用安全默认值。
- V003 在独立 MySQL 8 测试库重复执行两次，默认状态变为 APPROVED；原有两条图片记录及各自状态未被改写。
- V004 在独立 MySQL 8 测试库重复执行两次，`demo_resident` 默认 false、队列状态列为 `VARCHAR(16)`，原有 2 个域与 6 条队列记录保持不变。
- `npm run build:h5`、`npm run build:mp-weixin` 均通过。构建工具仍会输出 uni-app/Sass 的既有弃用提示，不影响产物。
- 真实接口冒烟已验证：独立游客会话、Audius 搜索返回 19 首曲目、曲目落库包含封面／署名／标签，官方流地址返回 MP3 音频。

正式账号体系和腾讯音乐商业曲库仍未提供接口；独立 Demo 的图片改为即时展示，公开运营前的内容管理方案需另行评估。
