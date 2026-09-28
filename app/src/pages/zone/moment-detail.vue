<template>
  <view class="page">
    <view class="nav sz-glass" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <text class="nav__title">Moment</text>
      <view class="nav__placeholder" />
    </view>

    <scroll-view v-if="moment" class="page__body" scroll-y>
      <view class="moment">
        <image
          v-if="imagePath"
          class="moment__image moment__image--photo"
          :src="imagePath"
          mode="widthFix"
        />
        <view
          v-else
          class="moment__image moment__image--placeholder"
          :style="{ backgroundColor: moment.color || '#aebdca' }"
        />

        <view class="moment__content">
          <view class="moment__user-row">
            <text class="moment__user" @click="goUserHome">@{{ moment.by }}</text>
            <text
              v-if="moment.userId === session.userId"
              class="moment__withdraw"
              @click="withdraw"
            >Withdraw</text>
          </view>
          <text v-if="moment.text" class="moment__text">{{ moment.text }}</text>
          <text class="moment__time">{{ moment.time }}</text>

          <view
            class="heart-button"
            :class="{
              'heart-button--active': moment.reaction === 'HEART',
              'heart-button--busy': reactionBusy,
            }"
            @click="toggleHeart"
          >
            <text class="heart-button__icon">{{ moment.reaction === 'HEART' ? '♥' : '♡' }}</text>
            <text class="heart-button__count">{{ moment.heartCount || 0 }}</text>
          </view>
        </view>
      </view>

      <view v-if="moment.music" class="music-section">
        <text class="music-section__eyebrow">ASSOCIATED TRACK</text>
        <view class="music-card sz-card">
          <view class="music-card__cover" :style="{ backgroundColor: moment.music.coverColor || '#aebdca' }">
            <image
              v-if="moment.music.coverUrl"
              class="music-card__cover-image"
              :src="moment.music.coverUrl"
              mode="aspectFill"
            />
            <text v-else class="music-card__note">♪</text>
          </view>
          <view class="music-card__copy">
            <text class="music-card__title">{{ moment.music.title }}</text>
            <text class="music-card__artist">{{ moment.music.artist }}</text>
            <view class="music-card__meta">
              <text>{{ formatDuration(moment.music.durationSec) }}</text>
              <text v-if="moment.music.attribution">{{ moment.music.attribution }}</text>
              <text v-else-if="moment.music.source">{{ moment.music.source }}</text>
            </view>
          </view>
        </view>
        <text class="music-section__note">Shared with this moment</text>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>

    <view v-else class="state">
      <text>{{ stateMessage }}</text>
      <button v-if="loadFailed" class="sz-btn-primary" @click="load">Retry</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { getMoment, loadImage, reactMoment, withdrawMoment } from '@/api/mock.js'
import { session } from '@/api/session.js'

const moment = ref(null)
const imagePath = ref('')
const stateMessage = ref('Loading moment…')
const loadFailed = ref(false)
const reactionBusy = ref(false)
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
let momentId = null
let generation = 0

onLoad((option) => {
  momentId = Number(option.id)
  if (!Number.isInteger(momentId) || momentId <= 0) {
    stateMessage.value = 'Moment unavailable'
    loadFailed.value = true
    return
  }
  load()
})
onUnload(() => { generation++ })

async function load() {
  const current = ++generation
  loadFailed.value = false
  stateMessage.value = 'Loading moment…'
  try {
    const data = await getMoment(momentId)
    if (current !== generation) return
    moment.value = data
    imagePath.value = ''
    if (data.imageUrl) {
      try {
        const path = await loadImage(data.imageUrl)
        if (current === generation) imagePath.value = path
      } catch { /* 保留动态占位色，歌曲信息仍可查看 */ }
    }
  } catch (e) {
    if (current !== generation) return
    moment.value = null
    stateMessage.value = e.message || 'Moment unavailable'
    loadFailed.value = true
  }
}

async function toggleHeart() {
  if (!moment.value || reactionBusy.value) return
  reactionBusy.value = true
  try {
    const state = await reactMoment(
      moment.value.id,
      moment.value.reaction === 'HEART' ? null : 'HEART',
    )
    moment.value.reaction = state.reaction
    moment.value.heartCount = state.heartCount
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' })
  } finally {
    reactionBusy.value = false
  }
}

async function withdraw() {
  if (!moment.value) return
  try {
    await withdrawMoment(moment.value.zoneId, moment.value.id)
    uni.showToast({ title: '已撤回', icon: 'none' })
    setTimeout(goBack, 350)
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' })
  }
}

function goUserHome() {
  const userId = moment.value?.userId
  if (!Number.isInteger(userId) || userId <= 0) return
  if (userId === session.userId) uni.switchTab({ url: '/pages/user/index' })
  else uni.navigateTo({ url: `/pages/user/home?userId=${userId}` })
}
function goBack() {
  uni.navigateBack({
    fail: () => uni.switchTab({ url: '/pages/index/index' }),
  })
}
function formatDuration(seconds = 0) {
  const safe = Math.max(0, Number(seconds) || 0)
  return `${Math.floor(safe / 60)}:${String(safe % 60).padStart(2, '0')}`
}
</script>

<style lang="scss" scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: $sz-bg;

  &__body {
    flex: 1;
    min-height: 0;
    height: 0;
    box-sizing: border-box;
  }
}

.nav {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 0 32rpx 20rpx;
  border-radius: 0;
  background: rgba(255,255,255,.58);
  z-index: 2;

  &__back {
    width: 60rpx;
    color: $sz-text;
    font-size: 56rpx;
    line-height: 1;
  }

  &__title {
    color: $sz-text;
    font-size: 30rpx;
    font-weight: 650;
  }

  &__placeholder { width: 60rpx; }
}

.moment {
  width: 100%;
  background: #fff;

  &__image {
    display: block;
    width: 100%;
    height: 690rpx;
  }

  &__image--photo { height: auto; }
  &__image--placeholder { background: #aebdca; }

  &__content { padding: 28rpx 38rpx 32rpx; }

  &__user-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
  }

  &__user {
    overflow: hidden;
    color: $sz-text;
    font-size: 26rpx;
    font-weight: 650;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__withdraw {
    flex-shrink: 0;
    color: $sz-text-tertiary;
    font-size: 22rpx;
  }

  &__text {
    display: block;
    margin-top: 18rpx;
    color: $sz-text-secondary;
    font-size: 24rpx;
    line-height: 1.55;
  }

  &__time {
    display: block;
    margin-top: 10rpx;
    color: $sz-text-tertiary;
    font-size: 21rpx;
  }
}

.heart-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  min-width: 92rpx;
  height: 56rpx;
  margin-top: 24rpx;
  padding: 0 20rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(106, 94, 130, .15);
  border-radius: 999rpx;
  color: $sz-text-tertiary;
  background: rgba(192, 184, 210, .12);

  &:active { transform: scale(.94); }
  &--active {
    color: #a65f72;
    border-color: rgba(166, 95, 114, .22);
    background: rgba(221, 177, 189, .24);
  }
  &--busy { opacity: .55; pointer-events: none; }
  &__icon { font-size: 33rpx; line-height: 1; }
  &__count { font-size: 22rpx; font-weight: 600; font-variant-numeric: tabular-nums; }
}

.music-section {
  padding: 44rpx 34rpx 0;

  &__eyebrow {
    display: block;
    margin: 0 6rpx 16rpx;
    color: $sz-text-tertiary;
    font-size: 19rpx;
    font-weight: 650;
    letter-spacing: 2rpx;
  }

  &__note {
    display: block;
    margin: 12rpx 8rpx 0;
    color: $sz-text-tertiary;
    font-size: 19rpx;
  }
}

.music-card {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 18rpx;
  border-radius: 22rpx;
  background: rgba(255,255,255,.86);

  &__cover {
    display: flex;
    flex: 0 0 116rpx;
    align-items: center;
    justify-content: center;
    width: 116rpx;
    height: 116rpx;
    overflow: hidden;
    border-radius: 16rpx;
  }

  &__cover-image { width: 100%; height: 100%; }
  &__note { color: rgba(255,255,255,.84); font-size: 44rpx; }

  &__copy {
    display: flex;
    flex: 1;
    min-width: 0;
    flex-direction: column;
  }

  &__title {
    overflow: hidden;
    color: $sz-text;
    font-size: 26rpx;
    font-weight: 650;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__artist {
    margin-top: 6rpx;
    overflow: hidden;
    color: $sz-text-secondary;
    font-size: 22rpx;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    gap: 14rpx;
    margin-top: 10rpx;
    overflow: hidden;
    color: $sz-text-tertiary;
    font-size: 19rpx;
    white-space: nowrap;
  }
}

.state {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 24rpx;
  color: $sz-text-tertiary;
  font-size: 24rpx;

  button { font-size: 22rpx; border-radius: 999rpx; }
}

.bottom-spacer { height: 80rpx; }
</style>
