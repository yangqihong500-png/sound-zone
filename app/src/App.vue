<script setup>
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 应用启动：后续可在此做登录态初始化、WS 连接等（见 docs/05 接入层设计）
  console.log('同频 SoundZone 启动')
})
</script>

<style lang="scss">
/**
 * 全局样式 v3（App.vue 中的 style 不允许使用 scoped）
 * 视觉规范：V1 蓝白撞色 + H 柔和钢蓝 + 中性玻璃拟态
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
  backdrop-filter: blur(28px) saturate(120%);
  -webkit-backdrop-filter: blur(28px) saturate(120%);
  border: 1rpx solid $sz-glass-border;
  border-radius: $sz-radius-lg;
  box-shadow: $sz-shadow-soft;
  box-sizing: border-box;
}

/* Clear glass：覆盖于图片上的标签与即时状态。 */
.sz-glass-clear {
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1rpx solid rgba(255, 255, 255, 0.42);
  box-sizing: border-box;
}

/* 图片信息层始终使用中性玻璃，避免封面颜色与品牌蓝互相污染。 */
.sz-glass-tinted {
  background: linear-gradient(180deg, rgba(24, 35, 50, 0.48), rgba(15, 23, 42, 0.74));
  backdrop-filter: blur(18px) saturate(118%);
  -webkit-backdrop-filter: blur(18px) saturate(118%);
  border-top: 1rpx solid rgba(255, 255, 255, 0.32);
  box-sizing: border-box;
}

button::after { border: none; }

/* 莫兰迪胶囊标签：低饱和浅底 */
.sz-tag {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: $sz-font-xs;
  background-color: $sz-primary-soft;
  color: $sz-text-secondary;
}

/* 胶囊选中态：主题色轻高亮 */
.sz-tag--active {
  background-color: $sz-accent;
  color: #ffffff;
}

/* 通用操作层级：主操作为 H 蓝实体，次操作为浅蓝胶囊，禁用态保持中性灰。 */
.sz-btn-primary,
.sz-btn-secondary {
  box-sizing: border-box;
  margin: 0;
  border-radius: 999rpx;
  font-weight: 600;
}

.sz-btn-primary {
  border: 1rpx solid rgba(255, 255, 255, 0.66);
  background-color: $sz-primary;
  box-shadow: 0 12rpx 28rpx rgba(59, 110, 168, 0.22);
  color: #ffffff;
}

.sz-btn-secondary {
  border: 1rpx solid $sz-primary-border;
  background-color: $sz-primary-soft;
  color: $sz-primary;
}

.sz-btn-primary[disabled],
.sz-btn-primary--disabled {
  border-color: transparent;
  background-color: rgba(0, 0, 0, 0.08);
  box-shadow: none;
  color: $sz-text-tertiary;
  opacity: 1;
}

/* #ifdef H5 */
/* H5 保留原生 tab 跳转逻辑，将底栏呈现为悬浮圆角矩形。 */
.uni-tabbar {
  left: 16px !important;
  right: 16px !important;
  bottom: calc(12px + env(safe-area-inset-bottom)) !important;
  width: auto !important;
  height: 64px !important;
  box-sizing: border-box;
  overflow: visible;
  border: 1px solid rgba(59, 110, 168, 0.1) !important;
  border-radius: 22px !important;
  background: rgba(255, 255, 255, 0.88) !important;
  backdrop-filter: blur(28px) saturate(120%) !important;
  -webkit-backdrop-filter: blur(28px) saturate(120%) !important;
  box-shadow: 0 12px 34px rgba(30, 55, 84, 0.13);
}

.uni-tabbar-border { display: none !important; }
.uni-tabbar__item { overflow: visible; }
.uni-tabbar__bd {
  height: 64px !important;
  padding: 7px 0 5px;
  box-sizing: border-box;
  overflow: visible;
}

.uni-tabbar__item:not(:nth-child(3)) .uni-tabbar__label {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  margin: 0 !important;
  font-size: 11px !important;
  line-height: 1.05 !important;
}

.uni-tabbar__item:not(:nth-child(3)) .uni-tabbar__label::before {
  width: 22px;
  height: 22px;
  content: '';
  flex: none;
  background-color: currentColor;
  -webkit-mask-position: center;
  mask-position: center;
  -webkit-mask-repeat: no-repeat;
  mask-repeat: no-repeat;
  -webkit-mask-size: contain;
  mask-size: contain;
}

.uni-tabbar__item:nth-child(2) .uni-tabbar__label::before {
  -webkit-mask-image: url('/static/tabbar/home.svg');
  mask-image: url('/static/tabbar/home.svg');
}

.uni-tabbar__item:nth-child(4) .uni-tabbar__label::before {
  -webkit-mask-image: url('/static/tabbar/me.svg');
  mask-image: url('/static/tabbar/me.svg');
}

.uni-tabbar__item:nth-child(3) .uni-tabbar__bd {
  padding: 0;
}

.uni-tabbar__item:nth-child(3) .uni-tabbar__label {
  position: relative;
  top: 0;
  width: 56px !important;
  min-width: 56px;
  max-width: 56px;
  height: 56px !important;
  min-height: 56px;
  max-height: 56px;
  margin: 0 auto !important;
  box-sizing: border-box;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 50%;
  background: $sz-primary;
  box-shadow: 0 8px 18px rgba(59, 110, 168, 0.28);
  color: #ffffff !important;
  font-size: 32px !important;
  font-weight: 300;
  line-height: 56px !important;
}
/* #endif */
</style>
