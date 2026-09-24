<template>
  <!-- 域卡片：首页/发现页共用（v2：玻璃拟态 + 当前播放轻突出） -->
  <view class="zone-card sz-glass" @click="goDetail">
    <view class="zone-card__header">
      <view class="zone-card__title-wrap">
        <view class="live-dot" />
        <text class="zone-card__name">{{ zone.name }}</text>
        <text v-if="zone.visibility === 'PRIVATE'" class="zone-card__lock">私密</text>
      </view>
      <text class="zone-card__listeners">{{ zone.listeners }} 在线</text>
    </view>

    <view v-if="zone.nowPlaying" class="zone-card__playing">
      <view class="zone-card__cover" :style="{ backgroundColor: zone.coverColor }">
        <text class="zone-card__cover-note">♪</text>
      </view>
      <view class="zone-card__track">
        <text class="zone-card__track-title">{{ zone.nowPlaying.title }}</text>
        <text class="zone-card__track-artist">{{ zone.nowPlaying.artist }}</text>
      </view>
    </view>
    <view v-else class="zone-card__idle">等待第一首歌上传</view>

    <view class="zone-card__footer">
      <text class="sz-tag">{{ zone.scene }}</text>
      <text class="zone-card__host">域主 @{{ zone.host }}</text>
    </view>
  </view>
</template>

<script setup>
/**
 * ZoneCard 域卡片组件（v2：2026-09-24 视觉规范）
 * @prop {Object} zone 域摘要数据（字段见 api/mock.js ZONES）
 * 玻璃卡片：主题名 / 封面 / 在线人数 / 当前播放曲名+歌手（决议 D9）
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
  padding: $sz-gap-md;
  margin-bottom: $sz-gap-lg;  /* 卡片之间留白充足 */

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

  &__lock {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
    border: 1rpx solid $sz-text-tertiary;
    border-radius: 999rpx;
    padding: 0 12rpx;
  }

  &__listeners {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }

  &__playing {
    display: flex;
    align-items: center;
    gap: $sz-gap-sm;
    background-color: rgba(255, 255, 255, 0.5);
    border-radius: $sz-radius-md;
    padding: $sz-gap-sm;
    margin-bottom: $sz-gap-sm;
  }

  &__idle {
    font-size: $sz-font-sm;
    color: $sz-text-tertiary;
    padding: $sz-gap-sm 0;
  }

  &__cover {
    width: 88rpx;
    height: 88rpx;
    border-radius: $sz-radius-md;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: $sz-shadow-soft;
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

/* 在线呼吸点：克制的冷灰蓝 */
.live-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background-color: $sz-accent;
}
</style>
