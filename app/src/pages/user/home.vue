<template>
  <view class="page">
    <!-- 顶部返回 -->
    <view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <text class="nav__title">主页</text>
      <view class="nav__back" />
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="state">
      <text class="state__text">加载中…</text>
    </view>

    <!-- 用户不存在 / id 无效 -->
    <view v-else-if="error" class="state">
      <text class="state__icon">?</text>
      <text class="state__text">{{ error }}</text>
      <button class="state__btn" @click="goBack">返回</button>
    </view>

    <!-- 正常展示 -->
    <view v-else-if="user" class="page__body">
      <view class="profile sz-card">
        <view class="profile__avatar" :style="{ backgroundColor: user.avatarColor }">
          <text class="profile__avatar-text">{{ (user.name || '?')[0].toUpperCase() }}</text>
        </view>
        <view class="profile__info">
          <view class="profile__name-row">
            <text class="profile__name">{{ user.name }}</text>
            <text v-if="isMe" class="profile__me-tag">我</text>
          </view>
          <text class="profile__slogan">在同频的人里，找到同审美的人</text>
        </view>
        <!-- 非本人显示关注按钮（决议 D7：轻入口关注上传者） -->
        <button
          v-if="!isMe"
          class="profile__follow"
          :class="{ 'profile__follow--done': followed }"
          @click="onFollow"
        >{{ followed ? '已关注' : '关注' }}</button>
      </view>

      <view class="stats sz-card">
        <view class="stats__item">
          <text class="stats__num">{{ user.stats.uploads }}</text>
          <text class="stats__label">上传</text>
        </view>
        <view class="stats__item">
          <text class="stats__num">{{ user.stats.likes }}</text>
          <text class="stats__label">获赞</text>
        </view>
        <view class="stats__item">
          <text class="stats__num">{{ user.stats.moments }}</text>
          <text class="stats__label">分享</text>
        </view>
        <view class="stats__item">
          <text class="stats__num">{{ user.stats.following }}</text>
          <text class="stats__label">关注</text>
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
import { CURRENT_USER_ID } from '@/api/constants.js'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const user = ref(null)
const loading = ref(true)
const error = ref('')
const followed = ref(false)

const isMe = computed(() => user.value && user.value.id === CURRENT_USER_ID)

onLoad(async (option) => {
  const userId = Number(option.userId)

  // 容错：userId 缺失或非法（非正整数）
  if (!option.userId || !Number.isInteger(userId) || userId <= 0) {
    loading.value = false
    error.value = '用户 ID 无效'
    return
  }

  try {
    user.value = await getUserProfile(userId)
  } catch (e) {
    // 后端 USER_NOT_FOUND(2003) 或其他错误 → 用户不存在
    error.value = '用户不存在或已注销'
  } finally {
    loading.value = false
  }
})

async function onFollow() {
  if (followed.value) {
    try {
      await unfollowUser(user.value.id)
      followed.value = false
    } catch (e) {
      uni.showToast({ title: e.message || '操作失败', icon: 'none' })
    }
  } else {
    try {
      await followUser(user.value.id)
      followed.value = true
      uni.showToast({ title: '已关注', icon: 'none' })
    } catch (e) {
      uni.showToast({ title: e.message || '关注失败', icon: 'none' })
    }
  }
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

  &__body {
    flex: 1;
    padding: 0 $sz-gap-md;
  }
}

.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
  padding-left: $sz-gap-md;
  padding-right: $sz-gap-md;

  &__back {
    font-size: 56rpx;
    line-height: 1;
    color: $sz-text;
    width: 60rpx;
  }

  &__title {
    font-size: $sz-font-lg;
    font-weight: 500;
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
  display: flex;
  align-items: center;
  gap: $sz-gap-md;
  margin: $sz-gap-md 0;

  &__avatar {
    width: 96rpx;
    height: 96rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__avatar-text {
    color: #ffffff;
    font-size: $sz-font-xl;
    font-weight: 500;
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  &__name-row {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }

  &__name {
    font-size: $sz-font-lg;
    font-weight: 500;
  }

  &__me-tag {
    font-size: $sz-font-xs;
    color: $sz-accent;
    border: 1rpx solid $sz-accent;
    border-radius: 999rpx;
    padding: 0 12rpx;
  }

  &__slogan {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }

  &__follow {
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

  &__item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__num {
    font-size: $sz-font-xl;
    font-weight: 500;
  }

  &__label {
    font-size: $sz-font-xs;
    color: $sz-text-secondary;
  }
}
</style>
