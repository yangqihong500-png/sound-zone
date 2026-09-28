<template>
  <view class="page">
    <view class="nav sz-glass" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <view class="nav__copy"><text class="nav__title">Moments</text><text class="nav__subtitle">Photos from the last 30 minutes</text></view>
      <view class="nav__back" />
    </view>

    <!-- 半小时内图片流（时间倒序，决议 D6/D9） -->
    <scroll-view class="page__body" scroll-y>
      <view
        v-for="(m, i) in moments"
        :key="m.id"
        class="feed-item"
        :class="{ 'feed-item--right': i % 2 }"
        @click="goMoment(m.id)"
      >
        <moment-card
          :moment="m"
          @user="goUserHome"
        />
        <!-- 单一心心互动：点击切换状态，右侧显示全域点赞数 -->
        <view class="heart-row">
          <view
            class="heart-button"
            :class="{
              'heart-button--active': m.reaction === 'HEART',
              'heart-button--busy': m._busy,
            }"
            @click.stop="onHeart(m)"
          >
            <text class="heart-button__icon">{{ m.reaction === 'HEART' ? '♥' : '♡' }}</text>
            <text class="heart-button__count">{{ m.heartCount || 0 }}</text>
          </view>
        </view>
      </view>
      <view v-if="!moments.length" class="empty">No moments shared in the last 30 minutes.</view>
      <view class="bottom-spacer" />
    </scroll-view>
  </view>
</template>

<script setup>
/**
 * 动态详情页 v2：2026-09-24 决议 D6/D7/D9
 * 半小时内图片流（时间倒序）+ 本人可撤回 + 心心互动，无评论区
 */
import { ref } from 'vue'
import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app'
import { getMomentFeed, reactMoment } from '@/api/mock.js'
import { session } from '@/api/session.js'
import MomentCard from '@/components/moment-card/moment-card.vue'

const moments = ref([])
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
let zoneId = null

let timer = null
let loading = false
onLoad((option) => { zoneId = Number(option.id) })
onShow(() => { refresh(); timer = setInterval(refresh, 10000) })
onHide(() => clearInterval(timer))
onUnload(() => clearInterval(timer))
async function refresh() {
  if (loading || !zoneId) return
  loading = true
  try { moments.value = await getMomentFeed(zoneId) }
  catch (e) { clearInterval(timer); uni.showToast({ title: e.message, icon: 'none' }) }
  finally { loading = false }
}
async function onHeart(m) {
  if (m._busy) return
  m._busy = true
  try {
    const state = await reactMoment(m.id, m.reaction === 'HEART' ? null : 'HEART')
    m.reaction = state.reaction
    m.heartCount = state.heartCount
  }
  catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
  finally { m._busy = false }
}
/**
 * 点击动态上传者 → 跳转用户主页（容错：id 无效不跳；本人跳"我的"）
 */
function goUserHome(userId) {
  if (!userId || !Number.isInteger(userId) || userId <= 0) {
    uni.showToast({ title: 'User unavailable', icon: 'none' })
    return
  }
  if (userId === session.userId) {
    uni.switchTab({ url: '/pages/user/index' })
    return
  }
  uni.navigateTo({ url: `/pages/user/home?userId=${userId}` })
}

function goBack() {
  uni.navigateBack()
}
function goMoment(momentId) {
  uni.navigateTo({ url: `/pages/zone/moment-detail?id=${momentId}` })
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
    padding: 24rpx 48rpx 0;
    box-sizing: border-box;
  }
}

.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 22rpx;
  padding-left: 40rpx;
  padding-right: 40rpx;
  border-radius: 0;
  background: rgba(255,255,255,.55);

  &__back {
    font-size: 56rpx;
    line-height: 1;
    color: $sz-text;
    width: 60rpx;
  }

  &__copy { display: flex; flex: 1; flex-direction: column; }
  &__title { font-size: 32rpx; font-weight: 600; }
  &__subtitle { font-size: 20rpx; color: $sz-text-tertiary; }
}

.feed-item {
  width: 88%;
  margin: 12rpx auto 46rpx 0;
  transform: rotate(-1.7deg);
  background: #fff;
  border-radius: 20rpx;
  box-shadow: $sz-shadow-soft;
  padding-bottom: 18rpx;
  &--right { margin-left: auto; margin-right: 0; transform: rotate(1.5deg); }
  :deep(.moment-card) { box-shadow: none; }
}

/* 单一心心互动：保留轻量感，并让状态和数量清晰可见 */
.heart-row {
  display: flex;
  margin: 0 20rpx;
  padding: 16rpx 8rpx 0;
  border-top: 1rpx solid rgba(0,0,0,.06);
}

.heart-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  min-width: 88rpx;
  height: 52rpx;
  padding: 0 18rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(106, 94, 130, .15);
  border-radius: 999rpx;
  color: $sz-text-tertiary;
  background: rgba(192, 184, 210, .12);
  transition: transform .16s ease, color .16s ease, background .16s ease;

  &:active {
    transform: scale(.94);
  }

  &--active {
    color: #a65f72;
    border-color: rgba(166, 95, 114, .22);
    background: rgba(221, 177, 189, .24);
  }

  &--busy {
    opacity: .55;
    pointer-events: none;
  }

  &__icon {
    font-size: 32rpx;
    line-height: 1;
  }

  &__count {
    min-width: 1ch;
    font-size: 21rpx;
    font-weight: 600;
    line-height: 1;
    font-variant-numeric: tabular-nums;
  }
}

.empty {
  text-align: center;
  color: $sz-text-tertiary;
  font-size: $sz-font-sm;
  padding: 80rpx 0;
}

.bottom-spacer {
  height: 100rpx;
}
</style>
