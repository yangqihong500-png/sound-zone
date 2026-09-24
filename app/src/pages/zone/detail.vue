<template>
  <view v-if="zone" class="detail">
    <!-- 顶部毛玻璃固定栏：返回 / 域名+在线人数 / 更多（决议 D9） -->
    <view class="nav sz-glass" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <view class="nav__title">
        <text class="nav__name">{{ zone.name }}</text>
        <text class="nav__listeners">{{ zone.listeners }} 在线</text>
      </view>
      <view class="nav__more" @click="onMore">···</view>
    </view>

    <scroll-view class="detail__body" scroll-y>
      <!-- 当前播放区：第一视觉（决议 D9） -->
      <view v-if="zone.nowPlaying" class="now-playing">
        <view class="now-playing__cover" :style="{ backgroundColor: zone.coverColor }">
          <text class="now-playing__note">♪</text>
        </view>
        <text class="now-playing__title">{{ zone.nowPlaying.title }}</text>
        <text class="now-playing__artist">{{ zone.nowPlaying.artist }} · @{{ zone.nowPlaying.by }} 上传</text>
        <!-- 极细播放进度条；同步播放无复杂控制（决议 D3） -->
        <progress
          class="now-playing__progress"
          :percent="zone.nowPlaying.progress"
          stroke-width="2"
          activeColor="#8c9bab"
          backgroundColor="rgba(0,0,0,0.08)"
        />
        <!-- 极简爱心收藏：收藏后上传者收到微光提示（决议 D7） -->
        <view class="now-playing__heart" :class="{ collected }" @click="onCollect">
          {{ collected ? '♥' : '♡' }}
        </view>
      </view>
      <view v-else class="now-playing now-playing--idle">
        <text class="now-playing__idle-text">还没有歌曲在播放，来上传第一首吧</text>
      </view>

      <!-- 动态分享区：主界面 1-2 张，可横滑，点击进入动态详情（决议 D6/D9） -->
      <view v-if="zone.moments.length" class="section">
        <view class="section__header" @click="goMoments">
          <text class="section__title">动态</text>
          <text class="section__more">半小时内 ›</text>
        </view>
        <scroll-view scroll-x enhanced :show-scrollbar="false" class="moment-scroll">
          <view class="moment-scroll__inner">
            <view
              v-for="m in zone.moments.slice(0, 2)"
              :key="m.id"
              class="moment-scroll__item"
              @click="goMoments"
            >
              <moment-card :moment="m" :own="m.by === CURRENT_USER_NAME" @withdraw="onWithdraw" />
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 歌单列表区：FIFO 按上传顺序，当前播放高亮（决议 D3/D9） -->
      <view class="section">
        <view class="section__header">
          <text class="section__title">歌单</text>
          <text class="section__more">{{ zone.queue.length }} 首待播</text>
        </view>
        <view class="sz-card queue-card">
          <queue-item
            v-if="zone.nowPlaying"
            :item="{ title: zone.nowPlaying.title, artist: zone.nowPlaying.artist, likes: 0, by: zone.nowPlaying.by }"
            :playing="true"
          />
          <queue-item
            v-for="(song, i) in zone.queue"
            :key="song.itemId"
            :item="song"
            :rank="i + 1"
          />
          <view v-if="!zone.queue.length && !zone.nowPlaying" class="queue-card__empty">
            歌单还是空的
          </view>
        </view>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>

    <!-- 底部悬浮毛玻璃操作栏：上传歌曲 / 上传图片（冷却置灰+倒计时，决议 D4/D9） -->
    <view class="action-bar sz-glass">
      <button
        class="action-bar__btn"
        :class="{ 'action-bar__btn--disabled': cooldown > 0 }"
        @click="onUploadSong"
      >
        <text v-if="cooldown > 0">{{ formatCooldown(cooldown) }} 后可上传</text>
        <text v-else>上传歌曲</text>
      </button>
      <button class="action-bar__btn action-bar__btn--primary" @click="showImagePopup = true">上传图片</button>
    </view>

    <!-- 两个半屏玻璃弹窗 -->
    <upload-song-popup
      :visible="showSongPopup"
      :cooldown="cooldown"
      :zone-id="zone.id"
      @close="showSongPopup = false"
      @uploaded="onSongUploaded"
      @toast="toast"
    />
    <upload-image-popup
      :visible="showImagePopup"
      :bind-track="zone.nowPlaying ? zone.nowPlaying.title : ''"
      :zone-id="zone.id"
      @close="showImagePopup = false"
      @uploaded="onImageUploaded"
      @toast="toast"
    />
  </view>
</template>

<script setup>
/**
 * 域内页（产品核心页面）v2：2026-09-24 决议 D3/D4/D6/D7/D9
 * 结构：毛玻璃顶栏 / 当前播放区（第一视觉+爱心收藏）/ 动态区（1-2 张横滑）/
 *       歌单列表（FIFO+高亮）/ 底部上传操作栏（冷却倒计时）/ 两个上传弹窗
 * 数据：api/mock.js（联调时替换为 GET /zones/{id} + WS）
 */
import { ref, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getZoneDetail, getCooldown, collectTrack, withdrawMoment,
} from '@/api/mock.js'
import { CURRENT_USER_NAME } from '@/api/constants.js'
import QueueItem from '@/components/queue-item/queue-item.vue'
import MomentCard from '@/components/moment-card/moment-card.vue'
import UploadSongPopup from '@/components/upload-song-popup/upload-song-popup.vue'
import UploadImagePopup from '@/components/upload-image-popup/upload-image-popup.vue'

const zone = ref(null)
const collected = ref(false)
const showSongPopup = ref(false)
const showImagePopup = ref(false)
const cooldown = ref(0)
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)

let cooldownTimer = null

onLoad(async (option) => {
  const data = await getZoneDetail(option.id)
  if (!data) {
    uni.showToast({ title: '域不存在', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  zone.value = data
  await refreshCooldown()
  // 冷却倒计时：每秒刷新（决议 D4）
  cooldownTimer = setInterval(refreshCooldown, 1000)
})

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})

async function refreshCooldown() {
  if (!zone.value) return
  cooldown.value = await getCooldown(zone.value.id)
}

/** 冷却中点击置灰按钮：轻量提示，不弹强窗 */
function onUploadSong() {
  if (cooldown.value > 0) {
    toast(`${formatCooldown(cooldown.value)} 后可上传`)
    return
  }
  showSongPopup.value = true
}

function onSongUploaded(item) {
  zone.value.queue.push(item)
  refreshCooldown()
}

function onImageUploaded(moment) {
  zone.value.moments.unshift(moment)
}

/** 收藏当前播放：爱心高亮 + 模拟上传者收到微光提示（决议 D7） */
async function onCollect() {
  collected.value = !collected.value
  if (collected.value) {
    await collectTrack(zone.value.id, zone.value.nowPlaying.trackId)
    toast('已收藏，上传者会收到微光提示')
  }
}

/** 更多：退出域 / 举报（决议 D9 顶部右侧更多按钮） */
function onMore() {
  uni.showActionSheet({
    itemList: ['退出域', '举报'],
    success: ({ tapIndex }) => {
      if (tapIndex === 0) {
        toast('已退出（全员退出后域自动消失）')
        setTimeout(() => uni.navigateBack(), 600)
      } else {
        toast('已收到举报，感谢反馈')
      }
    },
  })
}

async function onWithdraw(momentId) {
  await withdrawMoment(zone.value.id, momentId)
  zone.value.moments = zone.value.moments.filter((m) => m.id !== momentId)
  toast('已撤回')
}

function goMoments() {
  uni.navigateTo({ url: `/pages/zone/moments?id=${zone.value.id}` })
}

function goBack() {
  uni.navigateBack()
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

function formatCooldown(sec) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
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

/* 顶部毛玻璃固定栏：轻透，模糊下方内容 */
.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
  padding-left: $sz-gap-md;
  padding-right: $sz-gap-md;
  border-radius: 0;

  &__back {
    font-size: 56rpx;
    line-height: 1;
    color: $sz-text;
    width: 60rpx;
  }

  &__title {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__name {
    font-size: $sz-font-lg;
    font-weight: 500;
  }

  &__listeners {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }

  &__more {
    font-size: $sz-font-lg;
    color: $sz-text-secondary;
    width: 60rpx;
    text-align: right;
  }
}

/* 当前播放区：第一视觉，居中大封面 */
.now-playing {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $sz-gap-lg 0 $sz-gap-md;

  &__cover {
    width: 360rpx;
    height: 360rpx;
    border-radius: $sz-radius-lg;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: $sz-shadow-float;
    margin-bottom: $sz-gap-md;
  }

  &__note {
    color: #ffffff;
    font-size: 120rpx;
  }

  &__title {
    font-size: $sz-font-xl;
    font-weight: 500;
  }

  &__artist {
    font-size: $sz-font-sm;
    color: $sz-text-secondary;
    margin: 8rpx 0 $sz-gap-md;
  }

  &__progress {
    width: 70%;
  }

  /* 右下角极简爱心收藏 */
  &__heart {
    position: absolute;
    right: 12%;
    bottom: $sz-gap-md;
    font-size: 48rpx;
    color: $sz-text-tertiary;

    &.collected {
      color: $sz-accent;
    }
  }

  &--idle {
    padding: 80rpx 0;
  }

  &__idle-text {
    color: $sz-text-tertiary;
    font-size: $sz-font-sm;
  }
}

.section {
  margin-top: $sz-gap-md;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    padding: 0 8rpx;
    margin-bottom: $sz-gap-sm;
  }

  &__title {
    font-size: $sz-font-lg;
    font-weight: 500;
  }

  &__more {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }
}

.moment-scroll {
  white-space: nowrap;

  &__inner {
    display: inline-flex;
    gap: $sz-gap-sm;
  }

  &__item {
    width: 320rpx;
    flex-shrink: 0;
  }
}

.queue-card {
  padding: $sz-gap-sm;

  &__empty {
    text-align: center;
    color: $sz-text-tertiary;
    font-size: $sz-font-sm;
    padding: 40rpx 0;
  }
}

.bottom-spacer {
  height: 160rpx;
}

/* 底部悬浮毛玻璃操作栏 */
.action-bar {
  display: flex;
  gap: $sz-gap-md;
  padding: $sz-gap-sm $sz-gap-md calc(#{$sz-gap-sm} + env(safe-area-inset-bottom));
  border-radius: 0;

  &__btn {
    flex: 1;
    font-size: $sz-font-base;
    background-color: rgba(0, 0, 0, 0.06);
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

    /* 冷却置灰（决议 D4） */
    &--disabled {
      opacity: 0.45;
      color: $sz-text-secondary;
    }
  }
}
</style>
