<template>
  <view
    v-if="suspendedZone.active"
    class="floating-player"
    :class="{ 'floating-player--tab': tabPage }"
    @click="openZone"
  >
    <view class="floating-player__cover">
      <image
        v-if="suspendedZone.coverUrl"
        class="floating-player__image"
        :src="suspendedZone.coverUrl"
        mode="aspectFill"
      />
      <text v-else class="floating-player__note">♪</text>
      <view v-if="playback.playing" class="floating-player__pulse" />
    </view>
    <view class="floating-player__copy">
      <text class="floating-player__title">{{ suspendedZone.trackTitle }}</text>
      <text class="floating-player__meta">
        {{ suspendedZone.name }}{{ suspendedZone.artist ? ' · ' + suspendedZone.artist : '' }}
      </text>
    </view>
    <view class="floating-player__status">
      <text>{{ playback.playing ? '播放中' : playback.loading ? '连接中' : '已悬挂' }}</text>
    </view>
    <button class="floating-player__close" aria-label="退出域" @click.stop="exitZone">×</button>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { playback } from '@/services/player.js'
import { leaveCurrentZone, suspendedZone } from '@/services/zone-session.js'

const TAB_ROUTES = new Set([
  'pages/index/index',
  'pages/zone/create-entry',
  'pages/user/index',
])
const tabPage = computed(() => {
  const pages = getCurrentPages()
  return TAB_ROUTES.has(pages[pages.length - 1]?.route || '')
})

function openZone() {
  if (!suspendedZone.zoneId) return
  uni.navigateTo({ url: `/pages/zone/detail?id=${suspendedZone.zoneId}` })
}

async function exitZone() {
  await leaveCurrentZone()
  uni.showToast({ title: '已退出域', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.floating-player {
  position: fixed;
  z-index: 850;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(116rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  min-height: 104rpx;
  padding: 12rpx 14rpx;
  border: 1rpx solid rgba(255,255,255,.72);
  border-radius: 28rpx;
  background: rgba(250,251,253,.9);
  box-shadow: 0 16rpx 50rpx rgba(25,34,48,.18);
  backdrop-filter: blur(24px) saturate(130%);
  -webkit-backdrop-filter: blur(24px) saturate(130%);
  box-sizing: border-box;

  &--tab {
    bottom: calc(124rpx + env(safe-area-inset-bottom));
  }

  &__cover {
    position: relative;
    flex: 0 0 80rpx;
    width: 80rpx;
    height: 80rpx;
    overflow: visible;
    border-radius: 20rpx;
    background: linear-gradient(145deg, #dce6f0, #bdcce0);
  }

  &__image {
    width: 100%;
    height: 100%;
    border-radius: inherit;
  }

  &__note {
    display: flex;
    width: 100%;
    height: 100%;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 38rpx;
  }

  &__pulse {
    position: absolute;
    right: -5rpx;
    bottom: -5rpx;
    width: 18rpx;
    height: 18rpx;
    border: 4rpx solid #fff;
    border-radius: 50%;
    background: #5f78ff;
    box-shadow: 0 0 0 0 rgba(95,120,255,.4);
    animation: playback-pulse 1.8s ease-out infinite;
  }

  &__copy {
    display: flex;
    flex: 1;
    min-width: 0;
    margin-left: 18rpx;
    flex-direction: column;
    gap: 5rpx;
  }

  &__title,
  &__meta {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__title {
    color: #1c1c1e;
    font-size: 25rpx;
    font-weight: 700;
  }

  &__meta {
    color: #7a7f88;
    font-size: 20rpx;
  }

  &__status {
    flex: 0 0 auto;
    margin-left: 12rpx;
    padding: 7rpx 12rpx;
    border-radius: 999rpx;
    background: #edf0f5;
    color: #626a78;
    font-size: 18rpx;
  }

  &__close {
    display: flex;
    flex: 0 0 54rpx;
    width: 54rpx;
    height: 54rpx;
    min-height: 0;
    margin: 0 0 0 8rpx;
    padding: 0;
    align-items: center;
    justify-content: center;
    border: 0;
    border-radius: 50%;
    background: transparent;
    color: #646a73;
    font-size: 38rpx;
    font-weight: 300;
    line-height: 1;
  }

  &__close::after { border: 0; }
}

@keyframes playback-pulse {
  0% { box-shadow: 0 0 0 0 rgba(95,120,255,.42); }
  75%, 100% { box-shadow: 0 0 0 14rpx rgba(95,120,255,0); }
}
</style>
