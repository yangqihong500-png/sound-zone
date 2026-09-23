<template>
  <view class="page">
    <!-- 顶部问候区 -->
    <view class="hero">
      <text class="hero__title">此刻，谁在和你做同一件事</text>
      <text class="hero__subtitle">正在做的事，就是最好的歌单分类</text>
    </view>

    <!-- 主推域（取列表第一个，后续由推荐接口返回） -->
    <view v-if="zones.length" class="featured" @click="goDetail(zones[0].id)">
      <view class="featured__badge">正在热播</view>
      <text class="featured__name">{{ zones[0].name }}</text>
      <text class="featured__desc">
        {{ zones[0].listeners }} 人同频中 · ♪ {{ zones[0].nowPlaying.title }}
      </text>
      <view class="featured__cover" :style="{ backgroundColor: zones[0].coverColor }">
        <text class="featured__note">♪</text>
      </view>
    </view>

    <!-- 活跃域列表 -->
    <view class="section-title">此刻活跃的域</view>
    <zone-card v-for="zone in zones" :key="zone.id" :zone="zone" />
  </view>
</template>

<script setup>
/**
 * 首页（tabBar 页）
 * 内容：问候语 + 主推域 + 此刻活跃的域列表
 * 数据来源：api/mock.js getActiveZones()，后续替换为"域分发推荐"接口（docs/02 第 2 步）
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getActiveZones } from '@/api/mock.js'
import ZoneCard from '@/components/zone-card/zone-card.vue'

const zones = ref([])

onLoad(async () => {
  zones.value = await getActiveZones()
})

function goDetail(id) {
  uni.navigateTo({ url: `/pages/zone/detail?id=${id}` })
}
</script>

<style lang="scss" scoped>
.page {
  padding: $sz-gap-md;
}

.hero {
  display: flex;
  flex-direction: column;
  padding: $sz-gap-md 8rpx;

  &__title {
    font-size: $sz-font-xl;
    font-weight: 500;
  }

  &__subtitle {
    font-size: $sz-font-sm;
    color: $sz-text-secondary;
    margin-top: 8rpx;
  }
}

.featured {
  position: relative;
  background-color: $sz-primary-light;
  border-radius: $sz-radius-lg;
  padding: $sz-gap-lg;
  margin-bottom: $sz-gap-lg;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  &__badge {
    display: inline-block;
    align-self: flex-start;
    font-size: $sz-font-xs;
    color: #ffffff;
    background-color: $sz-accent;
    border-radius: 999rpx;
    padding: 4rpx 16rpx;
    margin-bottom: $sz-gap-sm;
  }

  &__name {
    font-size: $sz-font-xl;
    font-weight: 500;
    color: $sz-primary-dark;
  }

  &__desc {
    font-size: $sz-font-sm;
    color: $sz-text-secondary;
    margin-top: 8rpx;
  }

  &__cover {
    position: absolute;
    right: -30rpx;
    bottom: -30rpx;
    width: 180rpx;
    height: 180rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0.85;
  }

  &__note {
    color: #ffffff;
    font-size: 64rpx;
  }
}

.section-title {
  font-size: $sz-font-lg;
  font-weight: 500;
  margin-bottom: $sz-gap-md;
  padding: 0 8rpx;
}
</style>
