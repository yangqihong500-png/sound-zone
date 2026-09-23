<template>
  <view v-if="zone" class="detail">
    <!-- 自定义导航（pages.json 中 navigationStyle: custom） -->
    <view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <view class="nav__title">
        <view class="live-dot" />
        <text>{{ zone.name }}</text>
      </view>
      <text class="nav__listeners">{{ zone.listeners }} 人同频</text>
    </view>

    <scroll-view class="detail__body" scroll-y>
      <!-- 域主与风格信息 -->
      <view class="host-row">
        <text class="host-row__host">域主 @{{ zone.host }}</text>
        <view class="host-row__tags">
          <text v-for="t in zone.tags" :key="t" class="sz-tag">{{ t }}</text>
        </view>
      </view>
      <view v-if="zone.bannedTags.length" class="banned-row">
        本域禁止：{{ zone.bannedTags.join(' / ') }}
      </view>

      <!-- 正在播放 -->
      <view class="now-playing sz-card">
        <view class="now-playing__cover" :style="{ backgroundColor: zone.coverColor }">
          <text class="now-playing__note">♪</text>
        </view>
        <view class="now-playing__info">
          <text class="now-playing__title">{{ zone.nowPlaying.title }}</text>
          <text class="now-playing__artist">{{ zone.nowPlaying.artist }} · @{{ zone.nowPlaying.by }} 点播</text>
          <progress
            class="now-playing__progress"
            :percent="zone.nowPlaying.progress"
            stroke-width="3"
            activeColor="#31C27C"
            backgroundColor="#EEEEEE"
          />
        </view>
      </view>

      <!-- 播放队列 -->
      <view class="section sz-card">
        <view class="section__header">
          <text class="section__title">播放队列</text>
          <text class="section__extra">{{ zone.queue.length }} 首 · 点赞排序</text>
        </view>
        <queue-item v-for="song in zone.queue" :key="song.rank" :item="song" />
      </view>

      <!-- 碎片墙 -->
      <view class="section">
        <view class="section__header section__header--outside">
          <text class="section__title">碎片墙</text>
          <text class="section__extra">此刻的图与文，都挂在当下这首歌上</text>
        </view>
        <view class="moment-grid">
          <moment-card v-for="m in zone.moments" :key="m.id" :moment="m" class="moment-grid__item" />
        </view>
      </view>

      <!-- 底部留白，避免被操作栏遮挡 -->
      <view class="bottom-spacer" />
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="action-bar">
      <button class="action-bar__btn action-bar__btn--primary" @click="onRequest">点歌</button>
      <button class="action-bar__btn" @click="onMoment">发碎片</button>
    </view>
  </view>
</template>

<script setup>
/**
 * 域详情页 —— 产品核心页面（docs/01：域三件套）
 * 组成：同频电台（正在播放）+ 点歌台（队列）+ 碎片墙
 * Demo 数据为一次性拉取；真实实现为 WS 长连接：
 * 服务端权威时钟广播播放状态，点歌/红心实时同步（docs/05 接入层）
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getZoneDetail, requestSong } from '@/api/mock.js'
import QueueItem from '@/components/queue-item/queue-item.vue'
import MomentCard from '@/components/moment-card/moment-card.vue'

const zone = ref(null)
// 自定义导航需要手动避让状态栏
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)

onLoad(async (option) => {
  zone.value = await getZoneDetail(option.id)
  if (!zone.value) {
    uni.showToast({ title: '域不存在', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
  }
})

function goBack() {
  uni.navigateBack()
}

function onRequest() {
  // Demo：模拟点歌；真实流程见 api/mock.js requestSong 注释
  requestSong(zone.value.id, 'Demo 点歌')
  uni.showToast({ title: '已加入队列', icon: 'success' })
}

function onMoment() {
  uni.showToast({ title: 'Demo：碎片功能开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.detail {
  display: flex;
  flex-direction: column;
  height: 100vh;

  &__body {
    flex: 1;
    padding: 0 $sz-gap-md;
    box-sizing: border-box;
  }
}

.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
  padding-left: $sz-gap-md;
  padding-right: $sz-gap-md;
  background-color: $sz-card;

  &__back {
    font-size: 56rpx;
    line-height: 1;
    color: $sz-text;
    width: 60rpx;
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 10rpx;
    font-size: $sz-font-lg;
    font-weight: 500;
  }

  &__listeners {
    font-size: $sz-font-xs;
    color: $sz-accent;
    width: 120rpx;
    text-align: right;
  }
}

.live-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background-color: $sz-accent;
}

.host-row {
  display: flex;
  align-items: center;
  gap: $sz-gap-sm;
  padding: $sz-gap-md 8rpx 0;

  &__host {
    font-size: $sz-font-sm;
    color: $sz-text-secondary;
  }

  &__tags {
    display: flex;
    gap: 10rpx;
  }
}

.banned-row {
  font-size: $sz-font-xs;
  color: $sz-text-tertiary;
  padding: 8rpx 8rpx 0;
}

.now-playing {
  display: flex;
  align-items: center;
  gap: $sz-gap-md;
  margin-top: $sz-gap-md;

  &__cover {
    width: 120rpx;
    height: 120rpx;
    border-radius: $sz-radius-md;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__note {
    color: #ffffff;
    font-size: 52rpx;
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  &__title {
    font-size: $sz-font-lg;
    font-weight: 500;
  }

  &__artist {
    font-size: $sz-font-xs;
    color: $sz-text-secondary;
    margin: 6rpx 0 12rpx;
  }
}

.section {
  margin-top: $sz-gap-md;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin-bottom: $sz-gap-sm;

    &--outside {
      padding: 0 8rpx;
    }
  }

  &__title {
    font-size: $sz-font-lg;
    font-weight: 500;
  }

  &__extra {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }
}

.moment-grid {
  display: flex;
  flex-wrap: wrap;
  gap: $sz-gap-sm;

  &__item {
    width: calc(50% - #{$sz-gap-sm} / 2);
  }
}

.bottom-spacer {
  height: 140rpx;
}

.action-bar {
  display: flex;
  gap: $sz-gap-md;
  padding: $sz-gap-sm $sz-gap-md;
  padding-bottom: calc(#{$sz-gap-sm} + env(safe-area-inset-bottom));
  background-color: $sz-card;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.04);

  &__btn {
    flex: 1;
    font-size: $sz-font-base;
    background-color: $sz-bg;
    color: $sz-text;
    border-radius: 999rpx;

    &::after {
      border: none;
    }

    &--primary {
      background-color: $sz-primary;
      color: #ffffff;
      font-weight: 500;
    }
  }
}
</style>
