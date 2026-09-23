import { createSSRApp } from 'vue'
import App from './App.vue'

/**
 * uni-app Vue3 标准入口
 * 必须以工厂函数导出 createApp，由框架在 H5 / 小程序各端调用
 */
export function createApp() {
  const app = createSSRApp(App)
  return {
    app,
  }
}
