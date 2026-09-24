<template>
  <view class="page">
    <!-- 用户卡片 v2：功能型主页（去游戏化，无歌品值——2026-09-24 决议 D7） -->
    <view class="profile sz-card">
      <view class="profile__avatar" :style="{ backgroundColor: user.avatarColor }">
        <text class="profile__avatar-text">{{ user.name[0].toUpperCase() }}</text>
      </view>
      <view class="profile__info">
        <text class="profile__name">{{ user.name }}</text>
        <text class="profile__slogan">在同频的人里，找到同审美的人</text>
      </view>
    </view>

    <!-- 行为统计：上传/获赞/分享/关注（纯行为数据，非积分等级） -->
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
 * 我的（tabBar 页）v2：2026-09-24 决议 D7 去游戏化
 * 功能型主页：行为统计（上传/获赞/分享/关注）+ 功能入口
 * v1 的「歌品值」体系已取消，不再展示任何积分/等级/勋章
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCurrentUser } from '@/api/mock.js'

const user = ref({ name: '', avatarColor: '#8c9bab', stats: {} })
const menus = ['我的域', '我的上传', '我的关注', '我的收藏', '设置']

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
