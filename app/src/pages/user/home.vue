<template>
  <view class="page">
    <!-- 顶部返回 -->
    <view class="nav sz-glass" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <text class="nav__title">Profile</text>
      <view class="nav__back" />
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="state">
      <text class="state__text">Loading…</text>
    </view>

    <!-- 用户不存在 / id 无效 -->
    <view v-else-if="error" class="state">
      <text class="state__icon">?</text>
      <text class="state__text">{{ error }}</text>
      <button class="state__btn" @click="goBack">Back</button>
    </view>

    <!-- 正常展示 -->
    <view v-else-if="user" class="page__body">
      <view class="profile sz-card">
        <view class="profile__glow" :style="{ backgroundColor: user.avatarColor }" />
        <view class="profile__avatar" :style="{ backgroundColor: user.avatarColor }">
          <text class="profile__avatar-text">{{ (user.name || '?')[0].toUpperCase() }}</text>
        </view>
        <view class="profile__info">
          <view class="profile__name-row">
            <text class="profile__name">@{{ user.name }}</text>
            <text v-if="isMe" class="profile__me-tag">Me</text>
          </view>
          <text class="profile__slogan">someone is always listening with you</text>
        </view>
        <!-- 非本人显示关注按钮（决议 D7：轻入口关注上传者） -->
        <button
          v-if="!isMe"
          class="profile__follow"
          :class="{ 'profile__follow--done': followed }"
          @click="onFollow"
        >{{ followed ? 'Following' : 'Follow' }}</button>
      </view>

      <view class="stats sz-card">
        <view class="stats__item">
          <text class="stats__num">{{ user.stats.uploads }}</text>
          <text class="stats__label">Uploads</text>
        </view>
        <view class="stats__item">
          <text class="stats__num">{{ user.stats.likes }}</text>
          <text class="stats__label">Likes</text>
        </view>
        <view class="stats__item">
          <text class="stats__num">{{ user.stats.moments }}</text>
          <text class="stats__label">Shares</text>
        </view>
        <view class="stats__item">
          <text class="stats__num">{{ user.stats.following }}</text>
          <text class="stats__label">Following</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 用户主页 v2：从动态列表点击上传者跳转而来
 * @query userId 目标用户 ID（数字）
 * 容错：userId 缺失/非法 → 提示并返回；用户不存在（后端 2003）→ 展示空态
 */
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getUserProfile, followUser, unfollowUser } from '@/api/mock.js'
import { session } from '@/api/session.js'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const user = ref(null)
const loading = ref(true)
const error = ref('')
const followed = ref(false)
const followBusy = ref(false)

const isMe = computed(() => user.value && user.value.id === session.userId)

onLoad(async (option) => {
  const userId = Number(option.userId)

  // 容错：userId 缺失或非法（非正整数）
  if (!option.userId || !Number.isInteger(userId) || userId <= 0) {
    loading.value = false
    error.value = 'Invalid user ID'
    return
  }

  try {
    user.value = await getUserProfile(userId)
    followed.value = user.value.followed
  } catch (e) {
    // 后端 USER_NOT_FOUND(2003) 或其他错误 → 用户不存在
    error.value = e.message || 'Could not load profile'
  } finally {
    loading.value = false
  }
})

async function onFollow() {
  if (followBusy.value) return
  followBusy.value = true
  if (followed.value) {
    try {
      await unfollowUser(user.value.id)
      followed.value = false
    } catch (e) {
      uni.showToast({ title: e.message || 'Action failed', icon: 'none' })
    }
  } else {
    try {
      await followUser(user.value.id)
      followed.value = true
      uni.showToast({ title: 'Following', icon: 'none' })
    } catch (e) {
      uni.showToast({ title: e.message || 'Could not follow', icon: 'none' })
    }
  }
  followBusy.value = false
}

function goBack() {
  uni.navigateBack()
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
    padding: 0 32rpx;
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

  &__title {
    font-size: 32rpx;
    font-weight: 600;
  }
}

/* 加载中 / 用户不存在 空态 */
.state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: $sz-gap-md;

  &__icon {
    font-size: 80rpx;
    color: $sz-text-tertiary;
  }

  &__text {
    font-size: $sz-font-base;
    color: $sz-text-secondary;
  }

  &__btn {
    font-size: $sz-font-base;
    color: $sz-text;
    background-color: rgba(0, 0, 0, 0.06);
    border-radius: 999rpx;
    padding: 8rpx 48rpx;

    &::after {
      border: none;
    }
  }
}

.profile {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  margin: 32rpx 0 24rpx;
  min-height: 330rpx;
  justify-content: center;
  background: linear-gradient(135deg, rgba(168,184,200,.18), rgba(195,184,217,.18)), #fff;
  border-radius: 48rpx;
  &__glow { position: absolute; top: 45rpx; width: 180rpx; height: 180rpx; border-radius: 50%; filter: blur(32rpx); opacity: .45; }

  &__avatar {
    position: relative;
    width: 140rpx;
    height: 140rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    border: 4rpx solid rgba(255,255,255,.85);
    box-shadow: $sz-shadow-float;
  }

  &__avatar-text {
    color: #ffffff;
    font-size: 58rpx;
    font-weight: 600;
  }

  &__info {
    position: relative;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  &__name-row {
    display: flex;
    align-items: center;
    margin-top: 18rpx;
    gap: 12rpx;
  }

  &__name {
    font-size: 34rpx;
    font-weight: 600;
  }

  &__me-tag {
    font-size: $sz-font-xs;
    color: $sz-accent;
    border: 1rpx solid $sz-accent;
    border-radius: 999rpx;
    padding: 0 12rpx;
  }

  &__slogan {
    font-size: 22rpx;
    color: $sz-text-tertiary;
    font-style: italic;
  }

  &__follow {
    position: relative;
    margin-top: 20rpx;
    font-size: $sz-font-sm;
    color: #ffffff;
    background-color: $sz-primary;
    border-radius: 999rpx;
    padding: 8rpx 32rpx;
    flex-shrink: 0;

    &::after {
      border: none;
    }

    &--done {
      background-color: rgba(0, 0, 0, 0.1);
      color: $sz-text-secondary;
    }
  }
}

.stats {
  display: flex;
  border-radius: 32rpx;
  padding: 0;
  overflow: hidden;

  &__item {
    flex: 1;
    display: flex;
    position: relative;
    flex-direction: column;
    align-items: center;
    padding: 28rpx 0;
    &:not(:last-child)::after { content: ''; position: absolute; right: 0; top: 25%; height: 50%; width: 1rpx; background: rgba(0,0,0,.07); }
  }

  &__num {
    font-size: 34rpx;
    font-weight: 600;
  }

  &__label {
    font-size: 19rpx;
    color: $sz-text-tertiary;
  }
}
</style>
