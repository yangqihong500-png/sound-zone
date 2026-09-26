# 同频 SoundZone · 前端

uni-app（Vue3 + Vite），沿用现有 JavaScript 和 SCSS 风格，支持 H5 与微信小程序构建。

## 开发与构建

```bash
npm install
npm run dev:h5
npm run build:h5
npm run dev:mp-weixin
npm run build:mp-weixin
```

先按 [后端说明](../server/README.md) 启动服务。H5 开发默认请求当前页面主机的 `8080/api`（例如从局域网 IP 打开预览时，也会请求该 IP）；小程序默认仍为 `http://localhost:8080/api`，真机必须在本地 `.env.local` 配置可访问的后端地址：

```dotenv
VITE_API_BASE=http://localhost:8080/api
VITE_H5_SHARE_BASE=https://<实际部署域名>/
```

正式 H5 部署也必须显式配置 `VITE_API_BASE` 为 HTTPS 地址。独立 App 与小程序发布时仍需配置各自的平台标识和合法请求域名；构建通过不代表已经完成真机音频兼容验收。

## 页面与数据

- 首页／发现：公开活跃域、场景与关键词筛选。
- 建域／编辑：至少三首有效曲目、选择顺序、公开或私密、标签双模式；编辑仅修改主题信息。
- 域详情：服务端权威播放状态、FIFO、冷却、收藏、图片、动态、退出与举报。
- 动态详情：近三十分钟审核通过的图片、单选 emoji 与本人撤回。
- 用户主页／我的／我的内容：关注、上传、创建域、收藏、本人图片（含待审核）。

`api/transport.js` 负责 HTTP、错误结构和令牌；`request.js` 负责确保登录；`session.js` 获取独立会话。`mock.js` 为兼容旧引用保留文件名，内部全部调用真实接口，无固定用户 ID。

`services/player.js` 使用 uni-app 音频组件播放 Audius 或授权 HTTPS 音源，并按服务端时间对齐；宿主播放器桥仅为可选兼容层。`zone-session.js` 负责 WebSocket、重连、二十秒心跳和轮询兜底。没有音源时明确提示，客户端不自行跳过歌曲。具体约定见 [实施说明](../docs/06-implementation.md)。

## 视觉约定

沿用日式简约、黑白灰、莫兰迪主题色与玻璃拟态，优先复用 `uni.scss` 的设计变量；主要尺寸使用 rpx。页面通过 api 层取数，公共组件置于 `components/组件名/组件名.vue`。

## 常见问题

**HBuilderX 运行报 `Cannot find module @rollup/rollup-darwin-arm64` 或 esbuild 平台/版本不匹配**
原因：本项目依赖是用 x64 架构的 Node 安装的（终端 Node 走 Rosetta），而 HBuilderX 内置 arm64 原生 Node，需要各原生包的 arm64 版本，且**版本必须与主包严格一致**。
修复（重装 node_modules 后需重跑；注意 esbuild 必须钉版本）：

```bash
# 先查三个主包的版本
node -p "['esbuild','rollup','@parcel/watcher'].map(p=>p+': '+require('./node_modules/'+p+'/package.json').version).join('\n')"

# 再按查到的版本号安装对应原生包（下面是当前版本，以实际查询为准）
npm i @esbuild/darwin-arm64@0.20.2 @rollup/rollup-darwin-arm64@4.63.4 @parcel/watcher-darwin-arm64@2.6.0 --no-save --force
```

两个架构的原生包可以共存，互不影响。验证方式：`PATH="/usr/local/bin:$PATH" node node_modules/.bin/uni build`（用系统 arm64 Node 模拟 HBuilderX 环境构建）。
