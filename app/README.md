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
