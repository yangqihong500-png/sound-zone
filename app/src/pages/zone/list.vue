<template>
  <view class="page">
    <!-- 场景筛选条：横向滚动，对应"场景即标签"分发逻辑（docs/01） -->
    <scroll-view class="scene-bar" scroll-x enhanced :show-scrollbar="false">
      <view class="scene-bar__inner">
        <view
          v-for="scene in scenes"
          :key="scene"
          class="scene-bar__item"
          :class="{ active: scene === currentScene }"
          @click="switchScene(scene)"
        >
          {{ scene }}
        </view>
      </view>
    </scroll-view>

    <!-- 域列表 -->
    <view class="page__list">
      <zone-card v-for="zone in zones" :key="zone.id" :zone="zone" />
      <view v-if="!zones.length" class="empty">这个场景暂时没有活跃的域，去创建一个吧</view>
    </view>
  </view>
</template>

<script setup>
/**
 * 发现页（tabBar 页）—— 域列表
 * 功能：场景分类筛选 + 域卡片列表
 * 筛选为本地演示；真实实现由服务端按 用户画像 × 场景标签 推荐排序（docs/02）
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { SCENES, getZonesByScene } from '@/api/mock.js'
import ZoneCard from '@/components/zone-card/zone-card.vue'

const scenes = SCENES
const currentScene = ref('全部')
const zones = ref([])

onLoad(async () => {
  zones.value = await getZonesByScene(currentScene.value)
})

async function switchScene(scene) {
  currentScene.value = scene
  zones.value = await getZonesByScene(scene)
}
</script>

<style lang="scss" scoped>
.page {
  &__list {
    padding: $sz-gap-md;
  }
}

.scene-bar {
  background-color: $sz-card;
  white-space: nowrap;

  &__inner {
    display: inline-flex;
    gap: $sz-gap-sm;
    padding: $sz-gap-sm $sz-gap-md;
  }

  &__item {
    font-size: $sz-font-sm;
    color: $sz-text-secondary;
    padding: 8rpx 28rpx;
    border-radius: 999rpx;
    background-color: $sz-bg;

    &.active {
      color: #ffffff;
      background-color: $sz-primary;
      font-weight: 500;
    }
  }
}

.empty {
  text-align: center;
  color: $sz-text-tertiary;
  font-size: $sz-font-sm;
  padding: 80rpx 0;
}
</style>
