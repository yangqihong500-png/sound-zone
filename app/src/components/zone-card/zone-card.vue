<template>
  <!-- 域卡片：首页/发现页共用，点击跳转域详情 -->
  <view class="zone-card sz-card" @click="goDetail">
    <view class="zone-card__header">
      <view class="zone-card__title-wrap">
        <view class="live-dot" />
        <text class="zone-card__name">{{ zone.name }}</text>
      </view>
      <text class="zone-card__listeners">{{ zone.listeners }} 人同频</text>
    </view>

    <view class="zone-card__playing">
      <view class="zone-card__cover" :style="{ backgroundColor: zone.coverColor }">
        <text class="zone-card__cover-note">♪</text>
      </view>
      <view class="zone-card__track">
        <text class="zone-card__track-title">{{ zone.nowPlaying.title }}</text>
        <text class="zone-card__track-artist">{{ zone.nowPlaying.artist }} · @{{ zone.nowPlaying.by }} 点播</text>
      </view>
    </view>

    <view class="zone-card__footer">
      <text class="sz-tag">{{ zone.scene }}</text>
      <text class="zone-card__host">域主 @{{ zone.host }}</text>
    </view>
  </view>
</template>

<script setup>
/**
 * ZoneCard 域卡片组件
 * @prop {Object} zone 域摘要数据（字段见 api/mock.js ZONES）
 * 交互：点击整卡跳转详情页，路由参数 id
 */
const props = defineProps({
  zone: { type: Object, required: true },
})

function goDetail() {
  uni.navigateTo({ url: `/pages/zone/detail?id=${props.zone.id}` })
}
</script>

<style lang="scss" scoped>
.zone-card {
  margin-bottom: $sz-gap-md;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $sz-gap-sm;
  }

  &__title-wrap {
    display: flex;
    align-items: center;
    gap: 10rpx;
  }

  &__name {
    font-size: $sz-font-lg;
    font-weight: 500;
  }

  &__listeners {
    font-size: $sz-font-xs;
    color: $sz-accent;
  }

  &__playing {
    display: flex;
    align-items: center;
    gap: $sz-gap-sm;
    background-color: $sz-bg;
    border-radius: $sz-radius-md;
    padding: $sz-gap-sm;
    margin-bottom: $sz-gap-sm;
  }

  &__cover {
    width: 80rpx;
    height: 80rpx;
    border-radius: $sz-radius-sm;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__cover-note {
    color: #ffffff;
    font-size: 36rpx;
  }

  &__track {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  &__track-title {
    font-size: $sz-font-base;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__track-artist {
    font-size: $sz-font-xs;
    color: $sz-text-secondary;
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__host {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }
}

/* 直播中呼吸点 */
.live-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background-color: $sz-accent;
}
</style>
