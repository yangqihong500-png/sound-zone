<template>
  <view class="page">
    <!-- 顶部返回（决议 D9：简洁表单感） -->
    <view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <text class="nav__title">{{ editId ? 'Edit Zone' : 'Create Zone' }}</text>
      <view class="nav__back" />
    </view>

    <scroll-view class="page__body" scroll-y>
      <!-- 主题输入：极简横线 -->
      <view class="field">
        <view class="field__line">
          <input
            class="field__input"
            v-model="form.name"
            placeholder="Name your zone..."
            placeholder-class="placeholder"
            maxlength="64"
          />
        </view>
      </view>

      <!-- 场景选择（归入分发场景） -->
      <view class="section">
        <text class="section__label">Choose a scene</text>
        <view class="capsules">
          <view
            v-for="s in sceneOptions"
            :key="s"
            class="capsule"
            :class="{ 'capsule--active': form.scene === s }"
            @click="form.scene = s"
          ><text class="capsule__icon">{{ sceneIcon(s) }}</text>{{ sceneLabel(s) }}</view>
        </view>
      </view>

      <!-- 音乐过滤：先选模式，再从五类主题标签中选同一组过滤标签 -->
      <view class="section">
        <text class="section__label">Music filters</text>
        <view class="mode-switch">
          <view
            class="mode-switch__option"
            :class="{ 'mode-switch__option--active': form.filterMode === 'BAN' }"
            @click="!editId && (form.filterMode = 'BAN')"
          >Ban tagged songs</view>
          <view
            class="mode-switch__option"
            :class="{ 'mode-switch__option--active': form.filterMode === 'ALLOW' }"
            @click="!editId && (form.filterMode = 'ALLOW')"
          >Only allow tagged</view>
        </view>
        <text v-if="editId" class="field__hint">The music filter cannot be changed after creation.</text>
        <text v-else class="field__hint">Choose a filter mode, then select at least one tag below.</text>
      </view>

      <view class="section">
        <text class="section__label">Theme tags</text>
        <text class="field__hint">{{ form.filterMode === 'BAN' ? 'Songs with any selected tag will be blocked.' : form.filterMode === 'ALLOW' ? 'Only songs with a selected tag can play.' : 'Choose a music filter mode first.' }}</text>
        <view v-for="category in tagCategories" :key="category" class="tag-group">
          <text class="tag-group__name">{{ categoryLabel(category) }}</text>
          <view class="capsules">
            <view
              v-for="tag in (tagCatalog[category] || [])"
              :key="tag"
              class="capsule"
              :class="{ 'capsule--active': form.filterTags.includes(tag), 'capsule--disabled': !form.filterMode || editId }"
              @click="toggleFilterTag(tag)"
            >{{ tagLabel(tag) }}</view>
          </view>
        </view>
      </view>

      <!-- 私密设置（决议 D2：克制的控件，不用强警示视觉） -->
      <view v-if="!editId" class="section">
        <view class="privacy-row">
          <view><text class="section__label">Private Zone</text><text class="section__sub">Invite only access</text></view>
          <switch :checked="form.visibility === 'PRIVATE'" @change="onPrivacyChange" color="#8c9bab" />
        </view>
        <view v-if="form.visibility === 'PRIVATE'" class="field">
          <view class="field__line">
            <input
              class="field__input"
              v-model="form.password"
              placeholder="Set a password (optional)"
              placeholder-class="placeholder"
              maxlength="32" password
            />
          </view>
          <text class="field__hint">An invite link will be generated after creation.</text>
        </view>
      </view>

      <!-- 初始歌单（建域门槛：≥3 首，docs/02 第 1 步） -->
      <view v-if="!editId" class="section">
        <text class="section__label">Starter playlist <text class="section__sub">· at least 3 tracks</text></text>
        <input v-model="trackKeyword" class="field__input track-search" placeholder="Search tracks..." @input="loadTracks" />
        <text class="field__hint">Tracks play in selection order. Choose at least three tracks, each no longer than 10 minutes.</text>
        <view v-if="selectedTracks.length" class="selected-tracks">
          <text class="tag-group__name">Selected tracks · tap to remove</text>
          <view class="capsules">
            <view v-for="t in selectedTracks" :key="t.id" class="capsule capsule--active" :class="{ 'capsule--conflict': isTrackBlocked(t) }" @click="toggleTrack(t)">
              {{ t.title }} · {{ t.artist }} · {{ formatDuration(t.durationSec) }}<text v-if="isTrackBlocked(t)"> · blocked</text>
            </view>
          </view>
        </view>
        <view class="capsules">
          <view
            v-for="t in seedTracks"
            :key="t.id"
            class="capsule"
            :class="{ 'capsule--active': form.trackIds.includes(t.id), 'capsule--conflict': form.trackIds.includes(t.id) && isTrackBlocked(t) }"
            @click="toggleTrack(t)"
          >{{ t.title }} · {{ t.artist }} · {{ formatDuration(t.durationSec) }}</view>
        </view>
        <text class="field__hint">{{ form.trackIds.length }} selected</text>
        <text v-if="blockedTracks.length" class="field__hint field__hint--error">{{ blockedTracks.length }} selected track(s) conflict with this filter. Remove them or change the filter before creating.</text>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>

    <!-- 底部悬浮创建按钮（表单完成后高亮，决议 D9） -->
    <view class="create-bar">
      <button
        class="create-bar__btn"
        :class="{ 'create-bar__btn--ready': canCreate }"
        @click="onCreate"
        :disabled="submitting || !canCreate"
      >{{ submitting ? 'Saving…' : (editId ? 'Save Changes' : 'Create Zone') }}</button>
    </view>
  </view>
</template>

<script setup>
/**
 * 创建域页 v2：2026-09-24 决议 D2/D5/D9
 * 结构：主题输入 → 场景 → 过滤双模式 → 五类主题标签 → 私密设置 → 创建按钮
 * 初始歌单至少三首，按点选顺序入队；域主可复用本页修改域信息
 */
import { ref, computed, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { SCENES, getTagCatalog, createZone, updateZone, getZoneDetail, searchTracks } from '@/api/mock.js'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)

const tagCatalog = ref({})
const editId = ref(null)
const submitting = ref(false)
const trackKeyword = ref('')
let searchVersion = 0
const sceneOptions = SCENES.filter((s) => s !== '全部')
const tagCategories = ['语言', '年代', '风格', '场景', '情绪']
const sceneLabels = { 音乐: 'Music', 自习: 'Study', 健身: 'Fitness', 旅行: 'Travel', 日系: 'J-Pop', 电子: 'Electronic', 工作: 'Work', 手工: 'Craft', 深夜: 'Late Night' }
const sceneIcons = { 音乐: '♫', 自习: '✎', 健身: '⌁', 旅行: '✈', 日系: '✿', 电子: '⌁', 工作: '⌘', 手工: '◇', 深夜: '☾' }
const categoryLabels = { 语言: 'Language', 年代: 'Era', 风格: 'Genre', 场景: 'Scene', 情绪: 'Mood' }
const tagLabels = {
  华语: 'Mandarin', 粤语: 'Cantonese', 日语: 'Japanese', 韩语: 'Korean', 英语: 'English', 纯音乐: 'Instrumental',
  流行: 'Pop', 摇滚: 'Rock', 电子: 'Electronic', 说唱: 'Hip-Hop', 民谣: 'Folk', 爵士: 'Jazz', 古典: 'Classical', 抖音热曲: 'Trending',
  自习: 'Study', 健身: 'Fitness', 旅行: 'Travel', 通勤: 'Commute', 睡前: 'Bedtime', 工作: 'Work', 手工: 'Craft', 深夜: 'Late Night',
  舒缓: 'Calm', 治愈: 'Comforting', 亢奋: 'Energetic', 忧郁: 'Melancholy', 情歌: 'Romantic', 专注: 'Focus',
}
function sceneLabel(s) { return sceneLabels[s] || s }
function sceneIcon(s) { return sceneIcons[s] || '◌' }
function categoryLabel(s) { return categoryLabels[s] || s }
function tagLabel(s) { return tagLabels[s] || s }
function formatDuration(sec) {
  const value = Math.max(0, Number(sec) || 0)
  return `${Math.floor(value / 60)}:${String(value % 60).padStart(2, '0')}`
}

// 初始歌单候选曲目（简化：展示曲库前若干首，用户点选）
const seedTracks = ref([])
const selectedTracks = ref([])

onLoad(async (options) => {
  try {
    tagCatalog.value = await getTagCatalog()
    if (options.id) {
      editId.value = Number(options.id)
      const zone = await getZoneDetail(editId.value)
      Object.assign(form, { name: zone.name, scene: zone.scene, filterMode: zone.filterMode, filterTags: [...(zone.filterTags || [])], visibility: zone.visibility })
    } else await loadTracks()
  } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
})
async function loadTracks() {
  const version = ++searchVersion
  try {
    const tracks = await searchTracks(trackKeyword.value)
    if (version === searchVersion) seedTracks.value = tracks
  } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
}

const form = reactive({
  name: '',
  scene: '',
  filterMode: '',
  filterTags: [],
  visibility: 'PUBLIC',
  password: '',
  trackIds: [],
})

const blockedTracks = computed(() => selectedTracks.value.filter(isTrackBlocked))
const canCreate = computed(() =>
  form.name.trim().length > 0 && form.scene && (editId.value || (form.filterMode && form.filterTags.length > 0 && form.trackIds.length >= 3 && blockedTracks.value.length === 0))
)

function toggleFilterTag(tag) {
  if (editId.value || !form.filterMode) return
  const i = form.filterTags.indexOf(tag)
  i >= 0 ? form.filterTags.splice(i, 1) : form.filterTags.push(tag)
}

function isTrackBlocked(track) {
  if (!form.filterMode || !form.filterTags.length) return false
  const hit = (track.tags || []).some((tag) => form.filterTags.includes(tag))
  return form.filterMode === 'BAN' ? hit : !hit
}

function toggleTrack(track) {
  const i = form.trackIds.indexOf(track.id)
  if (i >= 0) {
    form.trackIds.splice(i, 1)
    selectedTracks.value.splice(i, 1)
  } else {
    form.trackIds.push(track.id)
    selectedTracks.value.push(track)
  }
}

function onPrivacyChange(e) {
  form.visibility = e.detail.value ? 'PRIVATE' : 'PUBLIC'
}

async function onCreate() {
  if (submitting.value) return
  if (!canCreate.value) {
    uni.showToast({ title: 'Name a scene and choose 3 tracks', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    if (editId.value) {
      await updateZone(editId.value, { name: form.name.trim(), scene: form.scene })
      uni.navigateBack()
      return
    }
    const zone = await createZone({
      name: form.name.trim(),
      scene: form.scene,
      filterMode: form.filterMode,
      filterTags: form.filterTags,
      visibility: form.visibility,
      password: form.password || null,
      trackIds: form.trackIds,
    })
    uni.showToast({ title: 'Zone created', icon: 'success' })
    uni.redirectTo({ url: `/pages/zone/detail?id=${zone.id}` })
  } catch (e) {
    uni.showToast({ title: e.message || 'Could not create zone', icon: 'none' })
  } finally { submitting.value = false }
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
    min-height: 0;
    height: 0;
    padding: 0 40rpx;
    box-sizing: border-box;
  }
}

.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 24rpx;
  padding-left: 40rpx;
  padding-right: 40rpx;
  background: rgba(255,255,255,.52);
  backdrop-filter: blur(28px);

  &__back {
    font-size: 56rpx;
    line-height: 1;
    color: $sz-text;
    width: 60rpx;
  }

  &__title {
    font-size: 33rpx;
    font-weight: 600;
  }
}

.field {
  margin-top: 34rpx;

  &__line {
    border-bottom: 2rpx solid rgba(0, 0, 0, 0.15);
    padding: 20rpx 4rpx;
  }

  &__input {
    font-size: 28rpx;
  }

  &__hint {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
    margin-top: 8rpx;
    display: block;

    &--error { color: #aa5555; }
  }
}

.placeholder {
  color: $sz-text-tertiary;
}

.section {
  margin-top: 48rpx;

  &__label {
    font-size: 28rpx;
    font-weight: 600;
    color: $sz-text;
    display: block;
    margin-bottom: 18rpx;
  }
  &__sub { display: block; font-size: 22rpx; color: $sz-text-tertiary; font-weight: 400; }
}

.tag-group {
  margin: 0 0 25rpx;

  &__name {
    font-size: 22rpx;
    color: $sz-text-secondary;
    font-weight: 500;
    display: block;
    margin-bottom: 8rpx;
  }
}

/* 低饱和胶囊标签，选中主题色轻高亮 */
.capsules {
  display: flex;
  flex-wrap: wrap;
  gap: 13rpx;

}

.capsule {
  font-size: 23rpx;
  color: $sz-text-secondary;
  padding: 11rpx 22rpx;
  border-radius: 999rpx;
  background-color: rgba(0, 0, 0, 0.055);
  border: 1rpx solid transparent;

  &--active {
    background-color: rgba(168,184,200,.3);
    color: $sz-text;
    border-color: rgba(168,184,200,.6);
  }
  &--disabled { opacity: .58; }
  &--conflict { border-color: #bd7777; color: #8f4444; }
  &__icon { margin-right: 7rpx; font-size: 25rpx; }
}

/* 过滤双模式切换：极简分段控件 */
.mode-switch {
  display: flex;
  gap: 0;
  padding: 6rpx;
  background: rgba(0,0,0,.055);
  border-radius: 18rpx;

  &__option {
    flex: 1;
    text-align: center;
    font-size: 21rpx;
    color: $sz-text-secondary;
    padding: 16rpx 0;
    border-radius: 14rpx;

    &--active {
      background-color: #fff;
      color: $sz-text;
      box-shadow: 0 3rpx 8rpx rgba(0,0,0,.06);
    }
  }
}

.privacy-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bottom-spacer {
  height: 190rpx;
}

.create-bar {
  padding: 24rpx 40rpx calc(24rpx + env(safe-area-inset-bottom));
  background: rgba(255,255,255,.52);
  backdrop-filter: blur(28px);

  &__btn {
    font-size: 28rpx;
    padding: 13rpx;
    border-radius: 999rpx;
    background-color: rgba(0, 0, 0, 0.08);
    color: $sz-text-tertiary;

    &::after {
      border: none;
    }

    &--ready {
      background-color: $sz-primary;
      color: #ffffff;
      font-weight: 500;
    }
  }
}
.track-search {
  border-bottom: 1rpx solid rgba(0,0,0,.15);
  padding: 16rpx 4rpx;
  margin-bottom: 10rpx;
}
.selected-tracks { margin: 22rpx 0; }
</style>
