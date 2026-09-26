<script setup>
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 应用启动：后续可在此做登录态初始化、WS 连接等（见 docs/05 接入层设计）
  console.log('同频 SoundZone 启动')
})
</script>

<style lang="scss">
/**
 * 全局样式 v2（App.vue 中的 style 不允许使用 scoped）
 * 视觉规范：2026-09-24 定稿——黑白灰低饱和 + 莫兰迪 + 玻璃拟态
 * 注意：backdrop-filter 仅 H5 端生效，小程序端自动降级为半透明+软阴影（docs/04 风险表）
 */
page {
  background-color: $sz-bg;
  color: $sz-text;
  font-size: $sz-font-base;
  line-height: 1.6;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica,
    'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* 普通卡片：白底 + 大圆角 + 极软阴影 */
.sz-card {
  background-color: $sz-card;
  border-radius: $sz-radius-lg;
  padding: $sz-gap-md;
  box-sizing: border-box;
  box-shadow: $sz-shadow-soft;
}

/* Frosted glass：只用于顶部栏、底部栏、悬浮按钮与半屏弹窗。 */
.sz-glass {
  background: $sz-glass-bg;
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border: 1rpx solid $sz-glass-border;
  border-radius: $sz-radius-lg;
  box-shadow: $sz-shadow-soft;
  box-sizing: border-box;
}

/* Clear glass：覆盖于图片上的标签与即时状态。 */
.sz-glass-clear {
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1rpx solid rgba(255, 255, 255, 0.32);
  box-sizing: border-box;
}

/* Tinted glass：承载图片上的主要信息。颜色由组件内主题色叠加。 */
.sz-glass-tinted {
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1rpx solid rgba(255, 255, 255, 0.24);
  box-sizing: border-box;
}

button::after { border: none; }

/* 莫兰迪胶囊标签：低饱和浅底 */
.sz-tag {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: $sz-font-xs;
  background-color: rgba(140, 155, 171, 0.15);
  color: $sz-text-secondary;
}

/* 胶囊选中态：主题色轻高亮 */
.sz-tag--active {
  background-color: $sz-accent;
  color: #ffffff;
}
</style>
