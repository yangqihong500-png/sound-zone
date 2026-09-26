<template>
  <view class="home">
    <view class="home__search">
      <text class="home__search-icon">⌕</text>
      <input v-model="keyword" class="home__search-input" placeholder="Search zones, artists, tracks..." placeholder-class="home__placeholder" confirm-type="search" />
    </view>

    <view v-if="featured && showScenes" class="compact sz-glass" @click="goFeatured">
      <image v-if="featured.nowPlaying?.coverUrl" class="compact__cover" :src="featured.nowPlaying.coverUrl" mode="aspectFill" />
      <view v-else class="compact__cover" :style="{ backgroundColor: featured.coverColor }">♫</view>
      <view class="compact__copy">
        <text class="compact__name">{{ featured.name }}</text>
        <text class="compact__track">{{ featured.nowPlaying?.title }} · {{ featured.nowPlaying?.artist }}</text>
      </view>
      <view class="compact__live"><view class="compact__dot" /> LIVE</view>
      <text class="compact__listeners">{{ featured.listeners }}</text>
    </view>

    <view v-if="featured" class="featured">
      <text class="eyebrow">FEATURED ZONE</text>
      <zone-card :zone="featured" size="featured" featured />
      <view v-if="peekZones.length" class="peek-row">
        <view v-for="zone in peekZones" :key="zone.id" class="peek-card" :style="{ backgroundColor: zone.coverColor }" @click="goZone(zone.id)">
          <image v-if="zone.nowPlaying?.coverUrl" class="peek-card__image" :src="zone.nowPlaying.coverUrl" mode="aspectFill" />
          <view class="peek-card__scrim" />
          <text>{{ zone.name }}</text>
        </view>
        <view class="peek-more" @click="goDiscover">More ↓</view>
      </view>
    </view>

    <view class="feed">
      <view class="feed__heading">
        <text class="eyebrow">EXPLORE ZONES</text>
        <text class="feed__count">{{ zones.length }} live now</text>
      </view>
      <scroll-view class="scene-bar" scroll-x enhanced :show-scrollbar="false">
        <view class="scene-bar__inner">
          <view v-for="scene in scenes" :key="scene" class="scene-bar__item" :class="{ 'scene-bar__item--active': scene === currentScene }" @click="switchScene(scene)">
            {{ sceneLabel(scene) }}
          </view>
        </view>
      </scroll-view>
      <view v-if="zones.length" class="feed__grid">
        <zone-card v-for="(zone, i) in zones" :key="zone.id" :zone="zone" :size="i % 3 === 0 ? 'tall' : 'short'" />
      </view>
      <view v-else class="empty">No live zones match your search.</view>
    </view>

    <view class="create-float sz-glass" @click="goCreate">＋ Create Zone</view>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onShow, onPageScroll, onTabItemTap } from '@dcloudio/uni-app'
import { SCENES, getZonesByScene } from '@/api/mock.js'
import ZoneCard from '@/components/zone-card/zone-card.vue'

const scenes = SCENES
const labels = { 全部: 'All', 音乐: 'Music', 自习: 'Study', 健身: 'Fitness', 旅行: 'Travel', 日系: 'J-Pop', 电子: 'Electronic', 工作: 'Work', 手工: 'Craft', 深夜: 'Late Night' }
const currentScene = ref('全部')
const zones = ref([])
const keyword = ref('')
const showScenes = ref(false)
const featured = computed(() => zones.value[0] || null)
const peekZones = computed(() => zones.value.slice(1, 3))
let searchVersion = 0
let searchTimer = null

onShow(loadZones)
watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadZones, 250)
})
onPageScroll(({ scrollTop }) => { showScenes.value = scrollTop > 220 })

// 点击 tabBar 的 Home 图标：清空搜索词并回到顶部，重置为初始 home。
// 与"手动清空搜索栏文字返回"的现有逻辑并存，两种方式都可用。
onTabItemTap(() => {
  keyword.value = ''
  uni.pageScrollTo({ scrollTop: 0, duration: 300 })
})

async function loadZones() {
  const version = ++searchVersion
  try {
    const data = await getZonesByScene(currentScene.value, keyword.value)
    if (version === searchVersion) zones.value = data
  } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
}
function switchScene(scene) {
  currentScene.value = scene
  loadZones()
}
function sceneLabel(scene) { return labels[scene] || scene }
function goCreate() { uni.navigateTo({ url: '/pages/zone/create' }) }
function goZone(id) { uni.navigateTo({ url: '/pages/zone/detail?id=' + id }) }
function goFeatured() { if (featured.value) goZone(featured.value.id) }
function goDiscover() { uni.switchTab({ url: '/pages/zone/list' }) }
</script>

<style lang="scss" scoped>
.home { padding: 24rpx 32rpx 280rpx; min-height: 100vh; box-sizing: border-box; background: $sz-bg; }
.home__search {
  height: 72rpx; display: flex; align-items: center; gap: 12rpx;
  margin: 0 8rpx 46rpx; border-bottom: 1rpx solid rgba(0,0,0,.15);
}
.home__search-icon { font-size: 38rpx; line-height: 1; color: $sz-text-secondary; }
.home__search-input { flex: 1; font-size: 28rpx; color: $sz-text; }
.home__placeholder { color: rgba(28,28,30,.35); font-weight: 300; }
.eyebrow { color: rgba(74,74,76,.62); font-size: 21rpx; font-weight: 600; letter-spacing: 2.5rpx; }
.featured { margin: 0 8rpx 60rpx; }
.featured .eyebrow { display: block; margin-bottom: 20rpx; }
.peek-row { display: flex; gap: 14rpx; margin-top: 24rpx; height: 124rpx; }
.peek-card, .peek-more { flex: 1; min-width: 0; border-radius: 26rpx; overflow: hidden; position: relative; }
.peek-card__image, .peek-card__scrim { position: absolute; inset: 0; width: 100%; height: 100%; }
.peek-card__scrim { background: linear-gradient(to top, rgba(0,0,0,.5), transparent 75%); }
.peek-card text { position: absolute; bottom: 12rpx; left: 12rpx; right: 12rpx; color: #fff; font-size: 18rpx; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.peek-more { display: flex; align-items: center; justify-content: center; background: rgba(28,28,30,.07); color: $sz-text-secondary; font-size: 20rpx; }
.feed__heading { display: flex; align-items: center; justify-content: space-between; margin: 0 8rpx 20rpx; }
.feed__count { color: $sz-text-tertiary; font-size: 21rpx; }
.scene-bar { width: 100%; white-space: nowrap; margin-bottom: 24rpx; }
.scene-bar__inner { display: inline-flex; gap: 12rpx; padding: 3rpx 2rpx; }
.scene-bar__item { flex-shrink: 0; border-radius: 999rpx; padding: 10rpx 24rpx; color: $sz-text-secondary; background: rgba(0,0,0,.055); font-size: 23rpx; font-weight: 500; }
.scene-bar__item--active { background: $sz-primary; color: #fff; }
.feed__grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 22rpx; align-items: start; }
.feed__grid :deep(.zone-card:nth-child(2n)) { margin-top: 36rpx; }
.empty { text-align: center; color: $sz-text-tertiary; font-size: 25rpx; padding: 80rpx 20rpx; }
.compact { position: fixed; top: 8rpx; left: 30rpx; right: 30rpx; z-index: 30; height: 110rpx; border-radius: 28rpx; display: flex; align-items: center; gap: 16rpx; padding: 12rpx; background: rgba(245,246,247,.78); }
.compact__cover { width: 72rpx; height: 72rpx; border-radius: 18rpx; flex-shrink: 0; display: flex; align-items: center; justify-content: center; color: #fff; }
.compact__copy { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.compact__name, .compact__track { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.compact__name { font-size: 24rpx; font-weight: 600; }
.compact__track { font-size: 19rpx; color: $sz-text-secondary; }
.compact__live { font-size: 17rpx; font-weight: 600; display: flex; align-items: center; gap: 5rpx; }
.compact__dot { width: 8rpx; height: 8rpx; border-radius: 50%; background: #f08a88; }
.compact__listeners { font-size: 19rpx; color: $sz-text-secondary; }
.create-float { position: fixed; left: 50%; transform: translateX(-50%); bottom: calc(120rpx + env(safe-area-inset-bottom)); z-index: 40; border-radius: 999rpx; padding: 20rpx 52rpx; color: $sz-text; font-size: 27rpx; font-weight: 600; white-space: nowrap; box-shadow: $sz-shadow-float; }
/* #ifdef H5 */
.compact { top: 54px; }
/* #endif */
</style>
