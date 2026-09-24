<template>
  <view class="page">
    <!-- 极简搜索栏（决议 D9：细横线 + 放大镜，点击展开） -->
    <view class="search-line" :class="{ 'search-line--open': searchOpen }" @click="searchOpen = true">
      <text class="search-line__icon">⌕</text>
      <input
        v-if="searchOpen"
        class="search-line__input"
        v-model="keyword"
        placeholder="搜索域主题、歌曲、标签"
        placeholder-class="placeholder"
        focus
        @blur="onSearchBlur"
      />
      <text v-else class="search-line__placeholder">搜索</text>
    </view>

    <!-- 上滑出现的分类标签栏（决议 D9：不常驻，滚动超过阈值后出现） -->
    <scroll-view
      v-if="showScenes"
      class="scene-bar"
      scroll-x
      enhanced
      :show-scrollbar="false"
    >
      <view class="scene-bar__inner">
        <view
          v-for="scene in scenes"
          :key="scene"
          class="scene-bar__item"
          :class="{ 'scene-bar__item--active': scene === currentScene }"
          @click="switchScene(scene)"
        >
          {{ scene }}
        </view>
      </view>
    </scroll-view>

    <!-- 推荐域列表（玻璃卡片） -->
    <view class="page__list">
      <view class="section-title">推荐域</view>
      <zone-card v-for="zone in filteredZones" :key="zone.id" :zone="zone" />
      <view v-if="!filteredZones.length" class="empty">没有匹配的域，创建一个吧</view>
    </view>

    <!-- 悬浮创建域按钮（首页唯一强操作入口，决议 D9） -->
    <view class="fab sz-glass" @click="goCreate">
      <text class="fab__text">创建域</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 首页（tabBar 页）v2：2026-09-24 决议 D9
 * 组成：极简搜索栏 + 上滑出现分类标签栏 + 推荐域玻璃卡片列表 + 悬浮「创建域」按钮
 * 数据：api/mock.js（联调时替换为 GET /zones/active）
 */
import { ref, computed } from 'vue'
import { onLoad, onPageScroll } from '@dcloudio/uni-app'
import { SCENES, getActiveZones, getZonesByScene } from '@/api/mock.js'
import ZoneCard from '@/components/zone-card/zone-card.vue'

const scenes = SCENES
const currentScene = ref('全部')
const zones = ref([])
const keyword = ref('')
const searchOpen = ref(false)
const showScenes = ref(false)

onLoad(async () => {
  zones.value = await getActiveZones()
})

/** 上滑（页面滚动）超过阈值后显示分类标签栏 */
onPageScroll(({ scrollTop }) => {
  showScenes.value = scrollTop > 80
})

async function switchScene(scene) {
  currentScene.value = scene
  keyword.value = ''
  zones.value = await getZonesByScene(scene)
}

/** 搜索：按域主题 / 当前播放歌曲 / 标签过滤（决议 D9） */
const filteredZones = computed(() => {
  if (!keyword.value.trim()) return zones.value
  const kw = keyword.value.trim().toLowerCase()
  return zones.value.filter((z) =>
    z.name.toLowerCase().includes(kw) ||
    z.tags.some((t) => t.toLowerCase().includes(kw)) ||
    (z.nowPlaying && z.nowPlaying.title.toLowerCase().includes(kw))
  )
})

function onSearchBlur() {
  if (!keyword.value.trim()) searchOpen.value = false
}

function goCreate() {
  uni.navigateTo({ url: '/pages/zone/create' })
}
</script>

<style lang="scss" scoped>
.page {
  padding: $sz-gap-md;
  padding-bottom: 280rpx; /* 为悬浮按钮 + tabBar 留位，确保最后一项不被遮挡 */

  &__list {
    margin-top: $sz-gap-md;
  }
}

/* 极简搜索栏：默认一条细横线 + 放大镜 */
.search-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.15);
  padding: 12rpx 4rpx;

  &__icon {
    color: $sz-text-tertiary;
    font-size: $sz-font-lg;
  }

  &__placeholder {
    color: $sz-text-tertiary;
    font-size: $sz-font-sm;
  }

  &__input {
    flex: 1;
    font-size: $sz-font-base;
  }
}

.placeholder {
  color: $sz-text-tertiary;
}

/* 分类标签栏：低饱和胶囊，选中主题色轻高亮 */
.scene-bar {
  white-space: nowrap;
  margin-top: $sz-gap-sm;

  &__inner {
    display: inline-flex;
    gap: $sz-gap-sm;
    padding: 4rpx;
  }

  &__item {
    font-size: $sz-font-sm;
    color: $sz-text-secondary;
    padding: 8rpx 28rpx;
    border-radius: 999rpx;
    background-color: rgba(0, 0, 0, 0.04);

    &--active {
      background-color: $sz-accent;
      color: #ffffff;
    }
  }
}

.section-title {
  font-size: $sz-font-lg;
  font-weight: 500;
  margin-bottom: $sz-gap-md;
  padding: 0 8rpx;
}

.empty {
  text-align: center;
  color: $sz-text-tertiary;
  font-size: $sz-font-sm;
  padding: 80rpx 0;
}

/* 悬浮圆形玻璃按钮：轻软阴影 + 透明悬浮感
 * 注意：首页是 tabBar 页面，底部有原生 tabBar（约 50px ≈ 100rpx），
 * 故 bottom 需抬高到 tabBar 之上，避免被遮挡；再加安全区适配全面屏 */
.fab {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: calc(110rpx + env(safe-area-inset-bottom));
  padding: 24rpx 64rpx;
  border-radius: 999rpx;
  box-shadow: $sz-shadow-float;
  z-index: 50;

  &__text {
    font-size: $sz-font-base;
    font-weight: 500;
    color: $sz-text;
  }
}
</style>
