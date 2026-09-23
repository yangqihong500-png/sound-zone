<template>
  <view class="page">
    <!-- 歌品值卡片：审美身份证（docs/02 成长体系） -->
    <view class="profile sz-card">
      <view class="profile__avatar" :style="{ backgroundColor: user.avatarColor }">
        <text class="profile__avatar-text">{{ user.name[0].toUpperCase() }}</text>
      </view>
      <view class="profile__info">
        <text class="profile__name">{{ user.name }}</text>
        <text class="profile__slogan">我的审美身份证</text>
      </view>
      <view class="profile__score">
        <text class="profile__score-num">{{ user.tasteScore }}</text>
        <text class="profile__score-label">歌品值</text>
      </view>
    </view>

    <!-- 数据概览 -->
    <view class="stats sz-card">
      <view class="stats__item">
        <text class="stats__num">{{ user.stats.requests }}</text>
        <text class="stats__label">点歌</text>
      </view>
      <view class="stats__item">
        <text class="stats__num">{{ user.stats.likes }}</text>
        <text class="stats__label">获赞</text>
      </view>
      <view class="stats__item">
        <text class="stats__num">{{ user.stats.moments }}</text>
        <text class="stats__label">碎片</text>
      </view>
    </view>

    <!-- 功能入口（Demo 占位） -->
    <view class="menu sz-card">
      <view v-for="item in menus" :key="item" class="menu__item" @click="onMenu(item)">
        <text>{{ item }}</text>
        <text class="menu__arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 我的（tabBar 页）
 * 展示歌品值与行为统计；真实数据来自反馈服务聚合（docs/02 审美反馈机制）
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCurrentUser } from '@/api/mock.js'

const user = ref({ name: '', avatarColor: '#31C27C', tasteScore: 0, stats: {} })
const menus = ['我的域', '我的碎片', '审美报告', '设置']

onLoad(async () => {
  user.value = await getCurrentUser()
})

function onMenu(item) {
  uni.showToast({ title: `Demo：${item}开发中`, icon: 'none' })
}
</script>

<style lang="scss" scoped>
.page {
  padding: $sz-gap-md;
}

.profile {
  display: flex;
  align-items: center;
  gap: $sz-gap-md;
  margin-bottom: $sz-gap-md;

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
    display: flex;
    flex-direction: column;
  }

  &__name {
    font-size: $sz-font-lg;
    font-weight: 500;
  }

  &__slogan {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }

  &__score {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__score-num {
    font-size: 48rpx;
    font-weight: 500;
    color: $sz-primary;
  }

  &__score-label {
    font-size: $sz-font-xs;
    color: $sz-text-secondary;
  }
}

.stats {
  display: flex;
  margin-bottom: $sz-gap-md;

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

.menu {
  padding: 0 $sz-gap-md;

  &__item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $sz-gap-md 0;
    font-size: $sz-font-base;
    border-bottom: 1rpx solid $sz-bg;

    &:last-child {
      border-bottom: none;
    }
  }

  &__arrow {
    color: $sz-text-tertiary;
    font-size: $sz-font-lg;
  }
}
</style>
