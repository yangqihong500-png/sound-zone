# 同频 SoundZone · 前端 Demo（app/）

基于 **uni-app（Vue3 + Vite）** 的跨端前端：一套代码同时支持 **H5** 与 **微信小程序**。
产品设计见 `../docs/01~04`，开发方案见 `../docs/05`。

## 快速开始

```bash
npm install          # 安装依赖（Node ≥ 18，推荐 22）

npm run dev:h5         # H5 开发预览（默认 http://localhost:5173）
npm run build:h5       # 构建 H5 产物 → dist/build/h5

npm run dev:mp-weixin    # 微信小程序开发（产物 → dist/dev/mp-weixin，用微信开发者工具导入）
npm run build:mp-weixin  # 构建微信小程序产物 → dist/build/mp-weixin
```

## 前后端联调

前端数据层已切换到真实后端（Spring Boot，见 `../server/`）。

1. 先在 VSCode 启动后端（端口 8080，前缀 `/api`），确认可访问：`http://localhost:8080/api/zones/active`
2. 在 HBuilderX 运行前端 H5
3. 后端 `CorsConfig` 已放行 `http://localhost:*` 与 `http://127.0.0.1:*`，跨域已处理

关键文件：
- `src/api/request.js`：统一请求层（baseURL=`http://localhost:8080/api`，响应解包 code=0）
- `src/api/constants.js`：联调常量（`CURRENT_USER_ID=4` 对应种子数据 octave 用户、`CURRENT_USER_NAME='octave'`）
- `src/api/mock.js`：暴露与后端 18 个接口对应的函数（文件名保留 mock，内部已走真实请求）

> ⚠️ Demo 无登录体系，userId 硬编码为 4（后端种子数据 octave 用户）。若重置后端数据库导致 ID 变化，需同步改 `constants.js`。

## 目录结构

```
app/
├── index.html                  # H5 入口模板
├── vite.config.js              # Vite 配置（uni 插件）
├── package.json
└── src/
    ├── main.js                 # 应用入口（createSSRApp 工厂）
    ├── App.vue                 # 根组件 + 全局样式（sz-card / sz-tag）
    ├── manifest.json           # 多端应用配置
    ├── pages.json              # 页面路由 + 底部 tabBar 配置
    ├── uni.scss                # 全局设计令牌（品牌色/字号/间距/圆角）
    ├── api/
    │   └── mock.js             # Demo 数据层（后端就绪后替换为 uni.request/WS）
    ├── components/             # 公共组件（easycom 自动注册，免 import 亦可）
    │   ├── zone-card/          #   域卡片（首页/发现页共用）
    │   ├── queue-item/         #   播放队列条目（点赞交互）
    │   └── moment-card/        #   碎片卡片（图文 + 配乐三元组）
    └── pages/
        ├── index/index.vue     # 首页：问候 + 主推域 + 活跃域列表（tab）
        ├── zone/list.vue       # 发现：场景筛选 + 域列表（tab）
        ├── zone/detail.vue     # 域详情：播放中 + 队列 + 碎片墙（核心页面）
        └── user/index.vue      # 我的：歌品值 + 行为统计（tab）
```

## 设计约定

- **样式**：统一使用 `uni.scss` 中的 `$sz-*` 变量（品牌绿 `#31C27C`、强调红 `#FF5A5F`），禁止在页面里随手写死色值
- **单位**：全部使用 `rpx`，多端自适应
- **组件**：新组件放入 `components/组件名/组件名.vue`，享受 easycom 自动注册
- **数据**：页面只允许从 `api/` 目录取数，不直接写死数据——后端（FastAPI）接入时只改 `api/` 层

## Demo 阶段说明

- 数据为本地 mock（`api/mock.js`），接口形态与后端约定一致
- 封面/碎片图片用色块占位，后续接腾讯云 COS
- 点歌/发碎片为 Toast 占位，真实交互走 WebSocket（见 docs/05 接入层设计）

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
