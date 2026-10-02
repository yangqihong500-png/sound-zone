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

      <!-- 域封面只在创建时确定：可选自定义，否则锁定第一首歌封面。 -->
      <view v-if="!editId" class="section cover-section">
        <text class="section__label">Zone cover <text class="section__sub">· optional</text></text>
        <view class="cover-picker" :style="{ backgroundColor: coverPreviewColor }">
          <image v-if="coverPreviewUrl" class="cover-picker__image" :src="coverPreviewUrl" mode="aspectFill" />
          <view v-else class="cover-picker__placeholder">
            <text class="cover-picker__note">♫</text>
            <text>First track cover</text>
          </view>
          <text v-if="customCoverPath" class="cover-picker__badge">Custom cover</text>
        </view>
        <view class="cover-actions">
          <button class="cover-actions__primary" @click="chooseZoneCover">{{ customCoverPath ? 'Choose again' : 'Add zone cover' }}</button>
          <button v-if="customCoverPath" class="cover-actions__remove" @click="removeZoneCover">Remove</button>
        </view>
        <text class="field__hint">If skipped, the first track cover is used. The cover is locked after creation.</text>
      </view>

      <!-- 场景决定发现页分发，创建与编辑都必须明确选择。 -->
      <view class="section scene-section">
        <text class="section__label">Choose a scene <text class="section__required">Required</text></text>
        <view class="capsules">
          <view
            v-for="s in sceneOptions"
            :key="s"
            class="capsule scene-capsule"
            :class="{ 'capsule--active': form.scene === s }"
            @click="selectScene(s)"
          ><text class="capsule__icon">{{ sceneIcon(s) }}</text>{{ sceneLabel(s) }}</view>
        </view>
        <text v-if="!form.scene" class="field__hint">Choose one scene before creating the zone.</text>
      </view>

      <!-- 快速创建的第二个必填项：恰好三首初始歌曲，按点选顺序进入 FIFO。 -->
      <view v-if="!editId" class="section">
        <text class="section__label">Starter playlist <text class="section__sub">· choose 3 tracks</text></text>
        <input v-model="trackKeyword" class="field__input track-search" placeholder="Search tracks..." @input="loadTracks" />
        <text class="field__hint">Tracks play in selection order. Each track must be no longer than 10 minutes.</text>
        <view v-if="selectedTracks.length" class="selected-tracks">
          <text class="track-list__label">Selected tracks · play order</text>
          <view class="track-list track-list--selected">
            <view
              v-for="(track, index) in selectedTracks"
              :key="track.id"
              class="track-row track-row--selected"
              :class="{ 'track-row--conflict': isTrackBlocked(track) }"
              @click="toggleTrack(track)"
            >
              <view class="track-row__cover" :style="{ backgroundColor: track.coverColor || '#A8B8C8' }">
                <image v-if="track.coverUrl" class="track-row__cover-image" :src="track.coverUrl" mode="aspectFill" />
                <text v-else class="track-row__note">♪</text>
              </view>
              <view class="track-row__info">
                <text class="track-row__title">{{ track.title }}</text>
                <text class="track-row__artist">{{ track.artist }}</text>
                <text class="track-row__duration">{{ formatDuration(track.durationSec) }}<text v-if="isTrackBlocked(track)"> · blocked by filter</text></text>
              </view>
              <view class="track-row__order">
                <text>{{ index + 1 }}</text>
                <text class="track-row__remove">×</text>
              </view>
            </view>
          </view>
        </view>
        <text v-if="selectedTracks.length" class="track-list__label track-list__label--results">Search results</text>
        <scroll-view
          v-if="availableSeedTracks.length"
          class="track-list track-list--results"
          :style="{ height: resultsListHeight + 'rpx' }"
          scroll-y
          enhanced
          :show-scrollbar="false"
        >
          <view
            v-for="track in availableSeedTracks"
            :key="track.id"
            class="track-row"
            :class="{ 'track-row--conflict': isTrackBlocked(track) }"
            @click="toggleTrack(track)"
          >
            <view class="track-row__cover" :style="{ backgroundColor: track.coverColor || '#A8B8C8' }">
              <image v-if="track.coverUrl" class="track-row__cover-image" :src="track.coverUrl" mode="aspectFill" />
              <text v-else class="track-row__note">♪</text>
            </view>
            <view class="track-row__info">
              <text class="track-row__title">{{ track.title }}</text>
              <text class="track-row__artist">{{ track.artist }}</text>
              <text class="track-row__duration">{{ formatDuration(track.durationSec) }}<text v-if="isTrackBlocked(track)"> · blocked by filter</text></text>
            </view>
            <text class="track-row__add">＋</text>
          </view>
        </scroll-view>
        <text v-else class="track-list__empty">No matching tracks</text>
        <text class="field__hint">{{ form.trackIds.length }}/3 selected</text>
        <text v-if="blockedTracks.length" class="field__hint field__hint--error">{{ blockedTracks.length }} selected track(s) conflict with this filter. Remove them or change the filter before creating.</text>
      </view>

      <view v-if="!editId" class="advanced-toggle" @click="toggleAdvancedSettings">
        <view>
          <text class="section__label">More settings</text>
          <text class="section__sub">Music filter and privacy</text>
        </view>
        <text class="advanced-toggle__icon">{{ advancedOpen ? '−' : '+' }}</text>
      </view>

      <view v-if="editId || advancedOpen" class="advanced-panel">
      <!-- 音乐过滤：默认不限制；启用过滤后再选标签。 -->
      <view class="section">
        <text class="section__label">Music filters</text>
        <view class="mode-switch">
          <view
            class="mode-switch__option"
            :class="{ 'mode-switch__option--active': form.filterMode === 'NONE' }"
            @click="selectFilterMode('NONE')"
          >No filter</view>
          <view
            class="mode-switch__option"
            :class="{ 'mode-switch__option--active': form.filterMode === 'BAN' }"
            @click="selectFilterMode('BAN')"
          >Ban tags</view>
          <view
            class="mode-switch__option"
            :class="{ 'mode-switch__option--active': form.filterMode === 'ALLOW' }"
            @click="selectFilterMode('ALLOW')"
          >Allow tags</view>
        </view>
        <text v-if="editId" class="field__hint">The music filter cannot be changed after creation.</text>
        <text v-else class="field__hint">No filter is the default. Ban and Allow require at least one tag.</text>
      </view>

      <view v-if="form.filterMode !== 'NONE'" class="section">
        <text class="section__label">Filter tags</text>
        <text class="field__hint">{{ filterRuleHint }}</text>
        <view v-for="category in tagCategories" :key="category" class="tag-group">
          <text class="tag-group__name">{{ categoryLabel(category) }}</text>
          <view class="capsules">
            <view
              v-for="tag in (tagCatalog[category] || [])"
              :key="tag"
              class="capsule"
              :class="{ 'capsule--active': form.filterTags.includes(tag), 'capsule--disabled': editId }"
              @click="toggleFilterTag(tag)"
            >{{ tagLabel(tag) }}</view>
          </view>
        </view>
      </view>

      <!-- 私密设置（决议 D2：克制的控件，不用强警示视觉） -->
      <view v-if="!editId" class="section">
        <view class="privacy-row">
          <view><text class="section__label">Private Zone</text><text class="section__sub">Invite only access</text></view>
          <switch :checked="form.visibility === 'PRIVATE'" @change="onPrivacyChange" color="#1C1C1E" />
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
    <suspended-zone-player />
  </view>
</template>

<script setup>
/**
 * 创建域页 v2：2026-09-24 决议 D2/D5/D9
 * 快速创建：域名 + 场景 + 恰好三首歌曲；标签过滤与隐私收进 More settings。
 * 初始歌曲按点选顺序入队；域主可复用本页修改域信息。
 */
import { ref, computed, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { SCENES, getTagCatalog, createZone, createZoneWithCover, updateZone, getZoneDetail, searchTracks, resolveMediaUrl } from '@/api/mock.js'
import { finishCreateFromTab } from '@/services/create-entry.js'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)

const tagCatalog = ref({})
const editId = ref(null)
const submitting = ref(false)
const advancedOpen = ref(false)
const trackKeyword = ref('')
const customCoverPath = ref('')
let searchVersion = 0
const sceneOptions = SCENES.filter((s) => s !== '全部')
const tagCategories = ['语言', '年代', '风格', '情绪']
const sceneLabels = { 音乐: 'Music', 自习: 'Study', 健身: 'Fitness', 旅行: 'Travel', 日系: 'J-Pop', 电子: 'Electronic', 工作: 'Work', 手工: 'Craft', 深夜: 'Late Night' }
const sceneIcons = { 音乐: '♫', 自习: '✎', 健身: '⌁', 旅行: '✈', 日系: '✿', 电子: '⌁', 工作: '⌘', 手工: '◇', 深夜: '☾' }
const categoryLabels = { 语言: 'Language', 年代: 'Era', 风格: 'Genre', 情绪: 'Mood' }
const tagLabels = {
  华语: 'Mandarin', 粤语: 'Cantonese', 日语: 'Japanese', 韩语: 'Korean', 英语: 'English', 纯音乐: 'Instrumental',
  流行: 'Pop', 摇滚: 'Rock', 电子: 'Electronic', 说唱: 'Hip-Hop', 民谣: 'Folk', 爵士: 'Jazz', 古典: 'Classical', 抖音热曲: 'Trending',
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
const coverPreviewUrl = computed(() => customCoverPath.value || resolveMediaUrl(selectedTracks.value[0]?.coverUrl))
const coverPreviewColor = computed(() => selectedTracks.value[0]?.coverColor || '#A8B8C8')

onLoad(async (options) => {
  try {
    tagCatalog.value = await getTagCatalog()
    if (options.id) {
      editId.value = Number(options.id)
      advancedOpen.value = true
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
  filterMode: 'NONE',
  filterTags: [],
  visibility: 'PUBLIC',
  password: '',
  trackIds: [],
})

const blockedTracks = computed(() => selectedTracks.value.filter(isTrackBlocked))
const availableSeedTracks = computed(() => seedTracks.value.filter((track) => !form.trackIds.includes(track.id)))
const resultsListHeight = computed(() => Math.min(availableSeedTracks.value.length * 144, 520))
const filterReady = computed(() => form.filterMode === 'NONE' || form.filterTags.length > 0)
const filterRuleHint = computed(() => form.filterMode === 'BAN'
  ? 'Songs with any selected tag will be blocked.'
  : 'Match one selected tag in every category you use.')
const canCreate = computed(() =>
  form.name.trim().length > 0 && form.scene && (editId.value || (filterReady.value && form.trackIds.length === 3 && blockedTracks.value.length === 0))
)

function selectFilterMode(mode) {
  if (editId.value) return
  form.filterMode = mode
  if (mode === 'NONE') form.filterTags.splice(0)
}

function toggleAdvancedSettings() {
  advancedOpen.value = !advancedOpen.value
}

function selectScene(scene) {
  form.scene = scene
}

function chooseZoneCover() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: ({ tempFilePaths }) => { customCoverPath.value = tempFilePaths?.[0] || '' },
  })
}

function removeZoneCover() {
  customCoverPath.value = ''
}

function toggleFilterTag(tag) {
  if (editId.value || form.filterMode === 'NONE') return
  const i = form.filterTags.indexOf(tag)
  i >= 0 ? form.filterTags.splice(i, 1) : form.filterTags.push(tag)
}

function isTrackBlocked(track) {
  if (form.filterMode === 'NONE' || !form.filterTags.length) return false
  const trackTags = track.tags || []
  if (form.filterMode === 'BAN') return trackTags.some((tag) => form.filterTags.includes(tag))
  return tagCategories.some((category) => {
    const selected = (tagCatalog.value[category] || []).filter((tag) => form.filterTags.includes(tag))
    return selected.length > 0 && !selected.some((tag) => trackTags.includes(tag))
  })
}

function toggleTrack(track) {
  const i = form.trackIds.indexOf(track.id)
  if (i >= 0) {
    form.trackIds.splice(i, 1)
    selectedTracks.value.splice(i, 1)
  } else {
    if (isTrackBlocked(track)) {
      uni.showToast({ title: form.filterMode === 'BAN' ? 'Blocked by selected tags' : 'Does not match every filter category', icon: 'none' })
      return
    }
    if (form.trackIds.length >= 3) {
      uni.showToast({ title: 'Choose exactly 3 tracks', icon: 'none' })
      return
    }
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
    uni.showToast({ title: !form.scene ? 'Choose a scene' : 'Name your zone and choose 3 tracks', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    if (editId.value) {
      await updateZone(editId.value, { name: form.name.trim(), scene: form.scene })
      uni.navigateBack()
      return
    }
    const payload = {
      name: form.name.trim(),
      scene: form.scene,
      filterMode: form.filterMode,
      filterTags: form.filterTags,
      visibility: form.visibility,
      password: form.password || null,
      trackIds: form.trackIds,
    }
    const zone = customCoverPath.value
      ? await createZoneWithCover(payload, customCoverPath.value)
      : await createZone(payload)
    uni.showToast({ title: 'Zone created', icon: 'success' })
    finishCreateFromTab()
    uni.redirectTo({ url: `/pages/zone/detail?id=${zone.id}&returnHome=1` })
  } catch (e) {
    uni.showToast({ title: e.message || 'Could not create zone', icon: 'none' })
  } finally { submitting.value = false }
}

function goBack() {
  if (editId.value) {
    uni.navigateBack({ fail: goHome })
    return
  }
  finishCreateFromTab()
  goHome()
}

function goHome() {
  uni.switchTab({
    url: '/pages/index/index',
    fail: () => uni.reLaunch({ url: '/pages/index/index' }),
  })
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
  background: $sz-glass-bg;
  border-bottom: 1rpx solid rgba(0,0,0,.08);
  backdrop-filter: blur(28px) saturate(120%);

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
    border-bottom: 2rpx solid $sz-control-border;
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
  &__required { display: inline; margin-left: 10rpx; font-size: 19rpx; color: $sz-brand; font-weight: 500; }
}

.cover-section { margin-top: 34rpx; }
.cover-picker {
  position: relative;
  width: 100%;
  height: 300rpx;
  overflow: hidden;
  border-radius: 30rpx;
  background: linear-gradient(145deg, rgba(255,255,255,.26), rgba(0,0,0,.08));
  box-shadow: $sz-shadow-soft;

  &__image { width: 100%; height: 100%; }
  &__placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    color: rgba(255,255,255,.82);
    font-size: 22rpx;
    background: linear-gradient(145deg, rgba(255,255,255,.18), rgba(0,0,0,.08));
  }
  &__note { font-size: 68rpx; font-weight: 300; }
  &__badge {
    position: absolute;
    left: 20rpx;
    bottom: 18rpx;
    padding: 7rpx 16rpx;
    border: 1rpx solid rgba(255,255,255,.56);
    border-radius: 999rpx;
    color: #fff;
    background: rgba(20,24,32,.46);
    backdrop-filter: blur(16px);
    font-size: 18rpx;
  }
}
.cover-actions {
  display: flex;
  gap: 14rpx;
  margin-top: 16rpx;

  button {
    margin: 0;
    border-radius: 999rpx;
    font-size: 22rpx;
    line-height: 1.4;
    padding: 14rpx 24rpx;
    &::after { border: none; }
  }
  &__primary { flex: 1; color: #fff; background: $sz-control; }
  &__remove { flex: 0 0 auto; color: $sz-text-secondary; background: rgba(0,0,0,.055); }
}

.advanced-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 44rpx;
  padding: 24rpx 4rpx;
  border-top: 1rpx solid rgba(0, 0, 0, 0.08);
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.08);

  .section__label { margin-bottom: 4rpx; }

  &__icon {
    width: 52rpx;
    text-align: center;
    font-size: 38rpx;
    line-height: 1;
    color: $sz-control;
  }
}

.advanced-panel {
  padding-bottom: 4rpx;
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
    background-color: $sz-control;
    color: #fff;
    border-color: $sz-control;
    box-shadow: 0 8rpx 18rpx rgba(0, 0, 0, 0.12);
  }
  &--disabled { opacity: .58; }
  &--conflict { border-color: #bd7777; color: #8f4444; }
  &__icon { margin-right: 7rpx; font-size: 25rpx; }
}

.scene-section { margin-top: 36rpx; }
.scene-capsule { transition: background-color .16s ease, color .16s ease, transform .16s ease; }
.scene-capsule:active { transform: scale(.97); }

/* 过滤模式切换：极简分段控件 */
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
      color: $sz-control;
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
  background: rgba(255,255,255,.86);
  border-top: 1rpx solid rgba(0,0,0,.08);
  backdrop-filter: blur(28px) saturate(120%);

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
      background-color: $sz-brand;
      color: #ffffff;
      font-weight: 600;
      box-shadow: 0 16rpx 34rpx rgba(29,78,216,.24), inset 0 1rpx 0 rgba(255,255,255,.22);
    }
  }
}
.track-search {
  border-bottom: 1rpx solid $sz-control-border;
  padding: 16rpx 4rpx;
  margin-bottom: 10rpx;
}
.selected-tracks { margin-top: 24rpx; }

.track-list {
  margin-top: 10rpx;

  &--results {
    max-height: 520rpx;
    overscroll-behavior: contain;
  }

  &--selected {
    background: $sz-control-soft;
    border-radius: 24rpx;
    padding: 0 16rpx;
  }

  &__label {
    display: block;
    font-size: 21rpx;
    font-weight: 500;
    color: $sz-text-secondary;
    margin-bottom: 4rpx;

    &--results { margin-top: 28rpx; }
  }

  &__empty {
    display: block;
    padding: 36rpx 0;
    text-align: center;
    font-size: 22rpx;
    color: $sz-text-tertiary;
  }
}

.track-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-height: 112rpx;
  padding: 16rpx 4rpx;
  border-bottom: 1rpx solid rgba(0,0,0,.07);

  &:last-child { border-bottom: 0; }

  &--selected .track-row__title { color: $sz-text; }
  &--conflict .track-row__duration { color: #9f5555; }

  &__cover {
    width: 92rpx;
    height: 92rpx;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    border-radius: 18rpx;
    background: linear-gradient(145deg, #A8B8C8, #C3B8D9);
  }

  &__cover-image { width: 100%; height: 100%; }
  &__note { color: rgba(255,255,255,.88); font-size: 34rpx; }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2rpx;
  }

  &__title,
  &__artist,
  &__duration {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__title { font-size: 25rpx; font-weight: 600; color: $sz-text; }
  &__artist { font-size: 22rpx; color: $sz-text-secondary; }
  &__duration { font-size: 19rpx; color: $sz-text-tertiary; }

  &__add {
    width: 52rpx;
    height: 52rpx;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: $sz-control;
    color: #ffffff;
    border: 1rpx solid rgba(255,255,255,.7);
    box-shadow: 0 8rpx 20rpx rgba(0,0,0,.14);
    font-size: 28rpx;
  }

  &__order {
    width: 52rpx;
    height: 52rpx;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    border-radius: 50%;
    background: $sz-control-muted;
    color: $sz-control;
    font-size: 21rpx;
    font-weight: 600;
  }

  &__remove {
    position: absolute;
    right: -8rpx;
    bottom: -8rpx;
    width: 28rpx;
    height: 28rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: #fff;
    color: $sz-text-secondary;
    font-size: 20rpx;
    box-shadow: 0 2rpx 8rpx rgba(0,0,0,.1);
  }
}
</style>
