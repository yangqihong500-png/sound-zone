<template>
  <view class="discover">
    <view class="discover__heading">
      <text class="discover__eyebrow">FIND YOUR FREQUENCY</text>
      <text class="discover__title">Live Zones</text>
      <text class="discover__intro">Listen together, wherever you are.</text>
    </view>
    <scroll-view class="scene-bar" scroll-x enhanced :show-scrollbar="false">
      <view class="scene-bar__inner">
        <view v-for="scene in scenes" :key="scene" class="scene-bar__item" :class="{ active: scene === currentScene }" @click="switchScene(scene)">{{ sceneLabel(scene) }}</view>
      </view>
    </scroll-view>
    <view v-if="zones.length" class="discover__grid">
      <zone-card v-for="(zone, i) in zones" :key="zone.id" :zone="zone" :size="i % 3 === 0 ? 'tall' : 'short'" />
    </view>
    <view v-else class="empty">No live zones in this scene yet.</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { SCENES, getZonesByScene } from '@/api/mock.js'
import ZoneCard from '@/components/zone-card/zone-card.vue'

const scenes = SCENES
const labels = { 全部: 'All', 音乐: 'Music', 自习: 'Study', 健身: 'Fitness', 旅行: 'Travel', 日系: 'J-Pop', 电子: 'Electronic', 工作: 'Work', 手工: 'Craft', 深夜: 'Late Night' }
const currentScene = ref('全部')
const zones = ref([])
onShow(() => switchScene(currentScene.value))
async function switchScene(scene) {
  currentScene.value = scene
  try { zones.value = await getZonesByScene(scene) }
  catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
}
function sceneLabel(scene) { return labels[scene] || scene }
</script>

<style lang="scss" scoped>
.discover { min-height: 100vh; box-sizing: border-box; padding: 36rpx 32rpx 160rpx; background: $sz-bg; }
.discover__heading { margin: 0 8rpx 34rpx; display: flex; flex-direction: column; }
.discover__eyebrow { color: $sz-text-tertiary; font-size: 20rpx; font-weight: 600; letter-spacing: 2.5rpx; }
.discover__title { color: $sz-text; font-size: 52rpx; font-weight: 700; line-height: 1.25; margin-top: 8rpx; }
.discover__intro { color: $sz-text-secondary; font-size: 25rpx; margin-top: 8rpx; }
.scene-bar { width: 100%; white-space: nowrap; margin-bottom: 24rpx; }
.scene-bar__inner { display: inline-flex; gap: 12rpx; padding: 4rpx 2rpx; }
.scene-bar__item { flex-shrink: 0; border-radius: 999rpx; padding: 10rpx 24rpx; color: $sz-text-secondary; background: rgba(0,0,0,.055); font-size: 23rpx; font-weight: 500; }
.scene-bar__item.active { background: $sz-primary; color: #fff; }
.discover__grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 22rpx; align-items: start; }
.discover__grid :deep(.zone-card:nth-child(2n)) { margin-top: 36rpx; }
.empty { text-align: center; color: $sz-text-tertiary; font-size: 25rpx; padding: 100rpx 20rpx; }
</style>
