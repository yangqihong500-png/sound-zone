<template>
  <view class="home">
    <view class="home__brand-bar">
      <view class="home__logo" aria-hidden="true">
        <view class="home__logo-bar home__logo-bar--short" />
        <view class="home__logo-bar home__logo-bar--tall" />
        <view class="home__logo-bar home__logo-bar--medium" />
      </view>
      <text class="home__brand">SoundZone</text>
    </view>

    <view class="home__search">
      <text class="home__search-icon">⌕</text>
      <input v-model="keyword" class="home__search-input" placeholder="Search zones, artists, tracks..." placeholder-class="home__placeholder" confirm-type="search" />
    </view>

    <view v-if="discoverActive" class="discover-toolbar">
      <view class="discover-toolbar__search">
        <text class="home__search-icon">⌕</text>
        <input v-model="keyword" class="home__search-input" placeholder="Search zones, artists, tracks..." placeholder-class="home__placeholder" confirm-type="search" />
      </view>
      <scroll-view class="scene-bar scene-bar--fixed" scroll-x enhanced :show-scrollbar="false">
        <view class="scene-bar__inner">
          <view v-for="scene in scenes" :key="'fixed-' + scene" class="scene-bar__item" :class="{ 'scene-bar__item--active': scene === currentScene }" @click="switchScene(scene)">
            {{ sceneLabel(scene) }}
          </view>
        </view>
      </scroll-view>
    </view>

    <view v-if="featured" class="featured">
      <text class="eyebrow">FEATURED ZONE</text>
      <zone-card :zone="featured" size="featured" featured />
      <view v-if="peekZones.length" class="peek-row">
        <view v-for="zone in peekZones" :key="zone.id" class="peek-card" :style="{ backgroundColor: zone.coverColor }" @click="goZone(zone.id)">
          <image v-if="zone.coverUrl" class="peek-card__image" :src="resolveMediaUrl(zone.coverUrl)" mode="aspectFill" />
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
      <scroll-view class="scene-bar" :class="{ 'scene-bar--hidden': discoverActive }" scroll-x enhanced :show-scrollbar="false">
        <view class="scene-bar__inner">
          <view v-for="scene in scenes" :key="scene" class="scene-bar__item" :class="{ 'scene-bar__item--active': scene === currentScene }" @click="switchScene(scene)">
            {{ sceneLabel(scene) }}
          </view>
        </view>
      </scroll-view>
      <view v-if="zones.length" class="feed__grid">
        <view
          v-for="(column, columnIndex) in zoneColumns"
          :key="columnIndex"
          class="feed__column"
          :class="{ 'feed__column--offset': columnIndex === 1 }"
        >
          <zone-card
            v-for="item in column"
            :key="item.zone.id"
            :zone="item.zone"
            :size="item.index % 3 === 0 ? 'tall' : 'short'"
          />
        </view>
      </view>
      <view v-else class="empty">No live zones match your search.</view>
    </view>
    <suspended-zone-player />
  </view>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { onShow, onPageScroll, onTabItemTap } from '@dcloudio/uni-app'
import { SCENES, getZonesByScene, resolveMediaUrl } from '@/api/mock.js'
import ZoneCard from '@/components/zone-card/zone-card.vue'
import { sceneLabel as sceneDisplayLabel } from '@/constants/scenes.js'

const scenes = SCENES
const currentScene = ref('全部')
const zones = ref([])
const keyword = ref('')
const discoverActive = ref(false)
const featured = computed(() => zones.value[0] || null)
const peekZones = computed(() => zones.value.slice(1, 3))
const zoneColumns = computed(() => zones.value.reduce((columns, zone, index) => {
  columns[index % 2].push({ zone, index })
  return columns
}, [[], []]))
let searchVersion = 0
let searchTimer = null
let pageScrollTop = 0
let feedOffsetTop = Number.POSITIVE_INFINITY
const discoverTriggerOffset = 64

onShow(loadZones)
onPageScroll(({ scrollTop }) => updateDiscoverState(scrollTop))
onMounted(() => {
  // #ifdef H5
  window.addEventListener('scroll', onH5Scroll, { passive: true })
  document.addEventListener('click', onH5TabClick, true)
  // #endif
})
onBeforeUnmount(() => {
  // #ifdef H5
  window.removeEventListener('scroll', onH5Scroll)
  document.removeEventListener('click', onH5TabClick, true)
  // #endif
})
function onH5Scroll() {
  // #ifdef H5
  updateDiscoverState(window.scrollY || document.documentElement.scrollTop || 0)
  // #endif
}
function updateDiscoverState(scrollTop) {
  pageScrollTop = scrollTop
  discoverActive.value = scrollTop >= feedOffsetTop - discoverTriggerOffset
}
function onH5TabClick(event) {
  // #ifdef H5
  const item = event.target?.closest?.('.uni-tabbar__item')
  if (item?.textContent?.trim() === 'Home') resetHome()
  // #endif
}
watch(keyword, (value) => {
  clearTimeout(searchTimer)
  // 关键词默认执行全局搜索；用户仍可在结果出来后主动点击场景标签继续缩小范围。
  if (value.trim()) currentScene.value = '全部'
  searchTimer = setTimeout(loadZones, 250)
})

// 点击 tabBar 的 Home 图标：恢复全部场景、清空搜索词并回到顶部。
// 与"手动清空搜索栏文字返回"的现有逻辑并存，两种方式都可用。
onTabItemTap(resetHome)
function resetHome() {
  const hadKeyword = !!keyword.value
  currentScene.value = '全部'
  keyword.value = ''
  if (!hadKeyword) loadZones()
  uni.pageScrollTo({ scrollTop: 0, duration: 300 })
}

async function loadZones() {
  const version = ++searchVersion
  try {
    const data = await getZonesByScene(currentScene.value, keyword.value)
    if (version === searchVersion) {
      zones.value = data
      await nextTick()
      measureFeed()
    }
  } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
}
function measureFeed() {
  // #ifdef H5
  const element = document.querySelector('.feed')
  if (element) {
    feedOffsetTop = element.getBoundingClientRect().top + (window.scrollY || document.documentElement.scrollTop || 0)
    updateDiscoverState(window.scrollY || document.documentElement.scrollTop || 0)
    return
  }
  // #endif
  uni.createSelectorQuery()
    .select('.feed')
    .boundingClientRect((rect) => {
      if (!rect) return
      feedOffsetTop = rect.top + pageScrollTop
      discoverActive.value = pageScrollTop >= feedOffsetTop - discoverTriggerOffset
    })
    .exec()
}
function switchScene(scene) {
  currentScene.value = scene
  loadZones()
}
function sceneLabel(scene) { return scene === '全部' ? 'All' : sceneDisplayLabel(scene) }
function goZone(id) { uni.navigateTo({ url: '/pages/zone/detail?id=' + id }) }
// "More ↓"：滚动到本页的 explore zones 部分（往下滑即 discover 内容）
function goDiscover() { uni.pageScrollTo({ selector: '.feed', duration: 300 }) }
</script>

<style lang="scss" scoped>
.home { padding: 0 32rpx 160rpx; min-height: 100vh; box-sizing: border-box; background: $sz-bg; }
.home__brand-bar {
  display: flex;
  min-height: 56rpx;
  margin: 0 8rpx 14rpx;
  padding-top: calc(var(--status-bar-height) + 20rpx);
  align-items: center;
  gap: 12rpx;
}
.home__logo {
  width: 44rpx;
  height: 44rpx;
  flex: 0 0 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
  border: 1rpx solid rgba(255,255,255,.72);
  border-radius: 13rpx;
  background: $sz-brand;
  box-shadow: 0 7rpx 18rpx rgba(29,78,216,.2), inset 0 1rpx 0 rgba(255,255,255,.24);
}
.home__logo-bar {
  width: 4rpx;
  border-radius: 999rpx;
  background: #fff;
  &--short { height: 12rpx; }
  &--tall { height: 26rpx; }
  &--medium { height: 18rpx; }
}
.home__brand {
  color: $sz-text;
  font-size: 46rpx;
  font-weight: 800;
  letter-spacing: -1.2rpx;
  line-height: 1.15;
}
.home__search {
  height: 72rpx; display: flex; align-items: center; gap: 12rpx;
  margin: 0 8rpx 40rpx; padding: 0 24rpx; box-sizing: border-box;
  border: 1rpx solid $sz-control-border; border-radius: 24rpx;
  background: rgba(255,255,255,.76); box-shadow: 0 10rpx 28rpx rgba(30,55,84,.05);
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
.peek-more { display: flex; align-items: center; justify-content: center; background: $sz-control-soft; color: $sz-control; font-size: 20rpx; font-weight: 600; }
.feed__heading { display: flex; align-items: center; justify-content: space-between; margin: 0 8rpx 20rpx; }
.feed__count { color: $sz-text-tertiary; font-size: 21rpx; }
.scene-bar { width: 100%; white-space: nowrap; margin-bottom: 24rpx; }
.scene-bar__inner { display: inline-flex; gap: 12rpx; padding: 3rpx 2rpx; }
.scene-bar__item { flex-shrink: 0; border: 1rpx solid transparent; border-radius: 999rpx; padding: 10rpx 24rpx; color: $sz-text-secondary; background: rgba(255,255,255,.82); font-size: 23rpx; font-weight: 500; }
.scene-bar__item--active { background: $sz-control; color: #fff; }
.feed__grid { display: flex; align-items: flex-start; gap: 22rpx; }
.feed__column { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 22rpx; }
.feed__column--offset { margin-top: 36rpx; }
.empty { text-align: center; color: $sz-text-tertiary; font-size: 25rpx; padding: 80rpx 20rpx; }
.discover-toolbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 60;
  padding: 10rpx 32rpx 12rpx;
  background: rgba(247,249,252,.88);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: 0 8rpx 24rpx rgba(28,28,30,.07);
}
.discover-toolbar__search {
  height: 62rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin: 0 8rpx 10rpx;
  padding: 0 20rpx;
  border: 1rpx solid $sz-control-border;
  border-radius: 22rpx;
  background: rgba(255,255,255,.82);
}
.scene-bar--fixed { margin-bottom: 0; }
.scene-bar--hidden { visibility: hidden; }
.discover-toolbar { top: var(--status-bar-height); }
</style>
