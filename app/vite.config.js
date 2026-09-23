import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// uni-app Vite 配置：插件会自动读取 src/pages.json 与 src/manifest.json
export default defineConfig({
  plugins: [uni()],
})
