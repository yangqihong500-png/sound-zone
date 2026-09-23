<script setup>
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 应用启动：后续可在此做登录态初始化、WS 连接等（见 docs/05 接入层设计）
  console.log('同频 SoundZone 启动')
})
</script>

<style lang="scss">
/**
 * 全局样式（App.vue 中的 style 不允许使用 scoped）
 * 设计令牌变量定义在 uni.scss，由框架自动注入
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

/* 通用卡片容器：全 App 统一的卡片观感 */
.sz-card {
  background-color: $sz-card;
  border-radius: $sz-radius-lg;
  padding: $sz-gap-md;
  box-sizing: border-box;
}

/* 通用场景标签小胶囊 */
.sz-tag {
  display: inline-block;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  font-size: $sz-font-xs;
  background-color: $sz-primary-light;
  color: $sz-primary-dark;
}
</style>
