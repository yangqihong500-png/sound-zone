<template>
  <view class="page">
    <view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <text class="nav__title">{{ editId ? 'Edit Zone' : 'Create Zone' }}</text>
      <view class="nav__back" />
    </view>

    <view class="progress">
      <view class="progress__copy">
        <text class="progress__eyebrow">STEP {{ currentStep + 1 }} OF {{ steps.length }}</text>
        <text class="progress__title">{{ currentStepData.label }}</text>
      </view>
      <view class="progress__steps" aria-label="Creation progress">
        <view
          v-for="(step, index) in steps"
          :key="step.key"
          class="progress__step"
          :class="{
            'progress__step--current': index === currentStep,
            'progress__step--complete': index < currentStep && isStepComplete(step.key),
          }"
          @click="goToStep(index)"
        />
      </view>
    </view>

    <swiper class="wizard" :current="currentStep" :duration="260" @change="onStepChange">
      <swiper-item v-for="step in steps" :key="step.key" class="wizard__item">
        <scroll-view class="step-scroll" scroll-y enhanced :show-scrollbar="false">
          <view v-if="step.key === 'name'" class="step-content">
            <view class="step-intro">
              <text class="step-intro__number">01</text>
              <text class="step-intro__title">Give the zone a name.</text>
              <text class="step-intro__description">Short, memorable and easy to recognize while people are listening together.</text>
            </view>
            <view class="name-card sz-glass-clear">
              <text class="field-label">ZONE NAME</text>
              <input
                v-model="form.name"
                class="name-input"
                placeholder="e.g. After-work living room"
                placeholder-class="placeholder"
                maxlength="64"
                confirm-type="next"
              />
              <view class="name-card__footer">
                <text>{{ form.name.trim().length }}/64</text>
                <text v-if="form.name.trim()" class="name-card__ready">Looks good</text>
              </view>
            </view>
            <view class="mini-preview">
              <text class="mini-preview__label">PREVIEW</text>
              <view class="mini-preview__card">
                <view class="mini-preview__mark">SZ</view>
                <view class="mini-preview__copy">
                  <text class="mini-preview__name">{{ form.name.trim() || 'Your zone name' }}</text>
                  <text class="mini-preview__meta">Live listening zone</text>
                </view>
              </view>
            </view>
          </view>

          <view v-else-if="step.key === 'cover'" class="step-content">
            <view class="step-intro">
              <text class="step-intro__number">02</text>
              <text class="step-intro__title">Choose its visual.</text>
              <text class="step-intro__description">Add a custom cover now, or let the first selected track become the zone cover.</text>
            </view>
            <view class="cover-picker" :style="{ backgroundColor: coverPreviewColor }">
              <image v-if="coverPreviewUrl" class="cover-picker__image" :src="coverPreviewUrl" mode="aspectFill" />
              <view v-else class="cover-picker__placeholder">
                <view class="cover-picker__symbol">♫</view>
                <text>First track cover</text>
              </view>
              <view class="cover-picker__glass">
                <text>{{ customCoverPath ? 'CUSTOM COVER' : 'AUTO COVER' }}</text>
                <text>{{ form.name.trim() || 'Untitled zone' }}</text>
              </view>
            </view>
            <view class="cover-actions">
              <button class="cover-actions__primary" @click="chooseZoneCover">{{ customCoverPath ? 'Choose another' : 'Add zone cover' }}</button>
              <button v-if="customCoverPath" class="cover-actions__remove" @click="removeZoneCover">Remove</button>
            </view>
            <view class="optional-note">
              <text class="optional-note__icon">✓</text>
              <text>This step is optional. The final cover is locked after creation.</text>
            </view>
          </view>

          <view v-else-if="step.key === 'scene'" class="step-content">
            <view class="step-intro">
              <text class="step-intro__number">{{ editId ? '02' : '03' }}</text>
              <text class="step-intro__title">What moment is this for?</text>
              <text class="step-intro__description">Choose the listening context. Music style belongs in filters, not in Scene.</text>
            </view>
            <view class="quick-scenes">
              <view
                v-for="scene in quickScenes"
                :key="scene.value"
                class="quick-scene"
                :class="{ 'quick-scene--active': form.scene === scene.value }"
                @click="selectScene(scene.value)"
              >
                <text class="quick-scene__icon">{{ scene.icon }}</text>
                <text>{{ scene.label }}</text>
              </view>
            </view>
            <view class="browse-scenes sz-glass-clear" @click="openScenePicker">
              <view class="browse-scenes__icon">⌕</view>
              <view class="browse-scenes__copy">
                <text class="browse-scenes__title">Browse all scenes</text>
                <text class="browse-scenes__sub">Search presets or create your own</text>
              </view>
              <text class="browse-scenes__arrow">›</text>
            </view>
            <view v-if="form.scene" class="scene-selection">
              <view class="scene-selection__icon">{{ selectedSceneMeta.icon }}</view>
              <view class="scene-selection__copy">
                <text class="scene-selection__label">SELECTED SCENE</text>
                <text class="scene-selection__name">{{ selectedSceneMeta.label }}</text>
              </view>
              <text class="scene-selection__check">✓</text>
            </view>
            <text v-else class="required-hint">Choose one scene to continue.</text>
          </view>

          <view v-else-if="step.key === 'tracks'" class="step-content step-content--tracks">
            <view class="step-intro step-intro--compact">
              <text class="step-intro__number">04</text>
              <text class="step-intro__title">Start with three songs.</text>
              <text class="step-intro__description">They enter the shared queue in the order you select them.</text>
            </view>
            <view class="track-progress">
              <view class="track-progress__copy">
                <text>STARTER PLAYLIST</text>
                <text>{{ form.trackIds.length }}/3</text>
              </view>
              <view class="track-progress__bar"><view :style="{ width: (form.trackIds.length / 3 * 100) + '%' }" /></view>
            </view>
            <view v-if="selectedTracks.length" class="selected-tracks">
              <view
                v-for="(track, index) in selectedTracks"
                :key="track.id"
                class="track-row track-row--selected"
                :class="{ 'track-row--conflict': isTrackBlocked(track) }"
                @click="toggleTrack(track)"
              >
                <text class="track-row__index">{{ index + 1 }}</text>
                <view class="track-row__cover" :style="{ backgroundColor: track.coverColor || '#A8B8C8' }">
                  <image v-if="track.coverUrl" class="track-row__cover-image" :src="resolveMediaUrl(track.coverUrl)" mode="aspectFill" />
                  <text v-else>♪</text>
                </view>
                <view class="track-row__info">
                  <text class="track-row__title">{{ track.title }}</text>
                  <text class="track-row__artist">{{ track.artist }} · {{ formatDuration(track.durationSec) }}</text>
                </view>
                <text class="track-row__action track-row__action--remove">×</text>
              </view>
            </view>
            <view class="track-search">
              <text class="track-search__icon">⌕</text>
              <input v-model="trackKeyword" class="track-search__input" placeholder="Search tracks..." @input="loadTracks" />
            </view>
            <view v-if="availableSeedTracks.length" class="track-results">
              <view
                v-for="track in availableSeedTracks"
                :key="track.id"
                class="track-row"
                :class="{ 'track-row--conflict': isTrackBlocked(track) }"
                @click="toggleTrack(track)"
              >
                <view class="track-row__cover" :style="{ backgroundColor: track.coverColor || '#A8B8C8' }">
                  <image v-if="track.coverUrl" class="track-row__cover-image" :src="resolveMediaUrl(track.coverUrl)" mode="aspectFill" />
                  <text v-else>♪</text>
                </view>
                <view class="track-row__info">
                  <text class="track-row__title">{{ track.title }}</text>
                  <text class="track-row__artist">{{ track.artist }} · {{ formatDuration(track.durationSec) }}</text>
                  <text v-if="isTrackBlocked(track)" class="track-row__blocked">Blocked by current filter</text>
                </view>
                <text class="track-row__action">＋</text>
              </view>
            </view>
            <text v-else class="track-empty">No matching tracks</text>
          </view>

          <view v-else-if="step.key === 'more'" class="step-content">
            <view class="step-intro step-intro--compact">
              <text class="step-intro__number">{{ editId ? '03' : '05' }}</text>
              <text class="step-intro__title">Fine-tune the zone.</text>
              <text class="step-intro__description">Optional controls for music boundaries and access.</text>
            </view>
            <view class="setting-card">
              <view class="setting-card__heading">
                <view>
                  <text class="setting-card__title">Music filters</text>
                  <text class="setting-card__sub">Keep the queue within your taste</text>
                </view>
                <text v-if="editId" class="setting-card__lock">Locked</text>
              </view>
              <view class="mode-switch">
                <view class="mode-switch__option" :class="{ 'mode-switch__option--active': form.filterMode === 'NONE' }" @click="selectFilterMode('NONE')">None</view>
                <view class="mode-switch__option" :class="{ 'mode-switch__option--active': form.filterMode === 'BAN' }" @click="selectFilterMode('BAN')">Ban</view>
                <view class="mode-switch__option" :class="{ 'mode-switch__option--active': form.filterMode === 'ALLOW' }" @click="selectFilterMode('ALLOW')">Allow</view>
              </view>
              <view v-if="form.filterMode !== 'NONE'" class="filter-groups">
                <view v-for="category in tagCategories" :key="category" class="tag-group">
                  <text class="tag-group__name">{{ categoryLabel(category) }}</text>
                  <scroll-view class="tag-group__scroll" scroll-x enhanced :show-scrollbar="false">
                    <view class="tag-group__row">
                      <view
                        v-for="tag in (tagCatalog[category] || [])"
                        :key="tag"
                        class="tag"
                        :class="{ 'tag--active': form.filterTags.includes(tag), 'tag--disabled': editId }"
                        @click="toggleFilterTag(tag)"
                      >{{ tagLabel(tag) }}</view>
                    </view>
                  </scroll-view>
                </view>
                <text v-if="!filterReady" class="required-hint">Choose at least one tag for this filter.</text>
              </view>
            </view>
            <view v-if="!editId" class="setting-card privacy-card">
              <view class="setting-card__heading">
                <view>
                  <text class="setting-card__title">Private Zone</text>
                  <text class="setting-card__sub">Only people with an invite can enter</text>
                </view>
                <switch :checked="form.visibility === 'PRIVATE'" @change="onPrivacyChange" color="#1C1C1E" />
              </view>
              <input v-if="form.visibility === 'PRIVATE'" v-model="form.password" class="password-input" placeholder="Optional password" maxlength="32" password />
            </view>
            <view class="review-card">
              <text class="review-card__eyebrow">READY TO CREATE</text>
              <view class="review-card__row"><text>Name</text><text>{{ form.name.trim() || 'Missing' }}</text></view>
              <view class="review-card__row"><text>Scene</text><text>{{ form.scene ? selectedSceneMeta.label : 'Missing' }}</text></view>
              <view v-if="!editId" class="review-card__row"><text>Starter tracks</text><text>{{ form.trackIds.length }}/3</text></view>
              <view v-if="!editId" class="review-card__row"><text>Access</text><text>{{ form.visibility === 'PRIVATE' ? 'Private' : 'Public' }}</text></view>
            </view>
            <text v-if="blockedTracks.length" class="required-hint">{{ blockedTracks.length }} selected track(s) conflict with this filter.</text>
          </view>
          <view class="step-spacer" />
        </scroll-view>
      </swiper-item>
    </swiper>

    <view class="wizard-actions">
      <button class="wizard-actions__secondary" @click="onSecondaryAction">{{ currentStep ? 'Back' : 'Cancel' }}</button>
      <button class="wizard-actions__primary" :class="{ 'wizard-actions__primary--ready': primaryReady }" :disabled="submitting" @click="onPrimaryAction">{{ primaryLabel }}</button>
    </view>

    <view v-if="scenePickerOpen" class="scene-picker-mask" @click.self="closeScenePicker">
      <view class="scene-picker">
        <view class="scene-picker__handle" />
        <view class="scene-picker__head">
          <view class="scene-picker__close" @click="closeScenePicker">×</view>
          <text class="scene-picker__title">Choose a scene</text>
          <text class="scene-picker__done" @click="closeScenePicker">Done</text>
        </view>
        <view class="scene-search">
          <text class="scene-search__icon">⌕</text>
          <input v-model="sceneKeyword" class="scene-search__input" placeholder="Search scenes..." />
        </view>
        <scroll-view class="scene-tabs" scroll-x enhanced :show-scrollbar="false">
          <view class="scene-tabs__row">
            <view v-for="category in sceneCategories" :key="category.key" class="scene-tab" :class="{ 'scene-tab--active': activeSceneCategory === category.key }" @click="activeSceneCategory = category.key">{{ category.label }}</view>
          </view>
        </scroll-view>
        <scroll-view class="scene-picker__list" scroll-y enhanced :show-scrollbar="false">
          <view v-if="filteredScenes.length" class="scene-grid">
            <view v-for="scene in filteredScenes" :key="scene.value" class="scene-option" :class="{ 'scene-option--active': form.scene === scene.value }" @click="selectScene(scene.value)">
              <text class="scene-option__icon">{{ scene.icon }}</text>
              <text>{{ scene.label }}</text>
            </view>
          </view>
          <view v-else class="scene-empty">
            <text>No preset matches “{{ sceneKeyword }}”.</text>
            <text>Create it as a custom scene below.</text>
          </view>
          <view class="custom-scene">
            <text class="custom-scene__label">CAN'T FIND IT?</text>
            <view class="custom-scene__form">
              <input v-model="customSceneDraft" class="custom-scene__input" placeholder="Type a custom scene" maxlength="32" />
              <button class="custom-scene__button" @click="useCustomScene">Use it</button>
            </view>
            <text class="custom-scene__hint">Custom scenes use the same existing Scene field.</text>
          </view>
        </scroll-view>
      </view>
    </view>
    <suspended-zone-player />
  </view>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { SCENE_CATALOG, SCENE_CATEGORIES, QUICK_SCENE_VALUES, getSceneMeta } from '@/constants/scenes.js'
import { getTagCatalog, createZone, createZoneWithCover, updateZone, getZoneDetail, searchTracks, resolveMediaUrl } from '@/api/mock.js'
import { finishCreateFromTab } from '@/services/create-entry.js'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const createSteps = [
  { key: 'name', label: 'Name' },
  { key: 'cover', label: 'Cover' },
  { key: 'scene', label: 'Scene' },
  { key: 'tracks', label: 'Songs' },
  { key: 'more', label: 'More settings' },
]
const editSteps = [
  { key: 'name', label: 'Name' },
  { key: 'scene', label: 'Scene' },
  { key: 'more', label: 'Review' },
]

const tagCatalog = ref({})
const editId = ref(null)
const submitting = ref(false)
const currentStep = ref(0)
const trackKeyword = ref('')
const customCoverPath = ref('')
const scenePickerOpen = ref(false)
const sceneKeyword = ref('')
const customSceneDraft = ref('')
const activeSceneCategory = ref('daily')
let searchVersion = 0

const steps = computed(() => editId.value ? editSteps : createSteps)
const currentStepData = computed(() => steps.value[currentStep.value] || steps.value[0])
const quickScenes = QUICK_SCENE_VALUES.map(getSceneMeta)
const sceneCategories = SCENE_CATEGORIES
const selectedSceneMeta = computed(() => getSceneMeta(form.scene))
const filteredScenes = computed(() => {
  const query = sceneKeyword.value.trim().toLowerCase()
  return SCENE_CATALOG.filter((scene) => query
    ? scene.label.toLowerCase().includes(query) || scene.value.includes(query)
    : scene.category === activeSceneCategory.value)
})
const tagCategories = ['语言', '年代', '风格', '情绪']
const categoryLabels = { 语言: 'Language', 年代: 'Era', 风格: 'Genre', 情绪: 'Mood' }
const tagLabels = {
  华语: 'Mandarin', 粤语: 'Cantonese', 日语: 'Japanese', 韩语: 'Korean', 英语: 'English', 纯音乐: 'Instrumental',
  流行: 'Pop', 摇滚: 'Rock', 电子: 'Electronic', 说唱: 'Hip-Hop', 民谣: 'Folk', 爵士: 'Jazz', 古典: 'Classical', 抖音热曲: 'Trending',
  舒缓: 'Calm', 治愈: 'Comforting', 亢奋: 'Energetic', 忧郁: 'Melancholy', 情歌: 'Romantic', 专注: 'Focus',
}
function categoryLabel(value) { return categoryLabels[value] || value }
function tagLabel(value) { return tagLabels[value] || value }
function formatDuration(sec) {
  const value = Math.max(0, Number(sec) || 0)
  return `${Math.floor(value / 60)}:${String(value % 60).padStart(2, '0')}`
}

const seedTracks = ref([])
const selectedTracks = ref([])
const coverPreviewUrl = computed(() => customCoverPath.value || resolveMediaUrl(selectedTracks.value[0]?.coverUrl))
const coverPreviewColor = computed(() => selectedTracks.value[0]?.coverColor || '#9DADBF')

const form = reactive({
  name: '',
  scene: '',
  filterMode: 'NONE',
  filterTags: [],
  visibility: 'PUBLIC',
  password: '',
  trackIds: [],
})

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

const blockedTracks = computed(() => selectedTracks.value.filter(isTrackBlocked))
const availableSeedTracks = computed(() => seedTracks.value.filter((track) => !form.trackIds.includes(track.id)))
const filterReady = computed(() => form.filterMode === 'NONE' || form.filterTags.length > 0)
const canCreate = computed(() => form.name.trim().length > 0 && form.scene && (editId.value || (filterReady.value && form.trackIds.length === 3 && blockedTracks.value.length === 0)))
const isLastStep = computed(() => currentStep.value === steps.value.length - 1)
const primaryReady = computed(() => isLastStep.value ? canCreate.value : isStepComplete(currentStepData.value.key))
const primaryLabel = computed(() => {
  if (submitting.value) return 'Saving…'
  if (isLastStep.value) return editId.value ? 'Save Changes' : 'Create Zone'
  return currentStepData.value.key === 'cover' ? 'Skip / Continue' : 'Continue'
})

function isStepComplete(key) {
  if (key === 'name') return form.name.trim().length > 0
  if (key === 'scene') return Boolean(form.scene)
  if (key === 'tracks') return form.trackIds.length === 3 && blockedTracks.value.length === 0
  if (key === 'more') return filterReady.value && blockedTracks.value.length === 0
  return true
}
function validationMessage(key) {
  if (key === 'name') return 'Name your zone first'
  if (key === 'scene') return 'Choose a scene first'
  if (key === 'tracks') return 'Choose exactly 3 tracks'
  if (key === 'more') return filterReady.value ? 'Resolve tracks blocked by the filter' : 'Choose at least one filter tag'
  return ''
}
function onStepChange(event) { currentStep.value = Number(event.detail.current || 0) }
function goToStep(index) { currentStep.value = index }
function onSecondaryAction() { currentStep.value > 0 ? currentStep.value -= 1 : goBack() }
function onPrimaryAction() {
  if (isLastStep.value) return onCreate()
  const key = currentStepData.value.key
  if (!isStepComplete(key)) {
    uni.showToast({ title: validationMessage(key), icon: 'none' })
    return
  }
  currentStep.value += 1
}
function openScenePicker() {
  sceneKeyword.value = ''
  customSceneDraft.value = ''
  activeSceneCategory.value = ['custom', 'legacy'].includes(selectedSceneMeta.value.category) ? 'daily' : selectedSceneMeta.value.category
  scenePickerOpen.value = true
}
function closeScenePicker() { scenePickerOpen.value = false }
function selectScene(scene) { form.scene = scene }
function useCustomScene() {
  const value = customSceneDraft.value.trim()
  if (!value) {
    uni.showToast({ title: 'Type a scene name first', icon: 'none' })
    return
  }
  form.scene = value
  closeScenePicker()
}
function chooseZoneCover() {
  uni.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'], success: ({ tempFilePaths }) => { customCoverPath.value = tempFilePaths?.[0] || '' } })
}
function removeZoneCover() { customCoverPath.value = '' }
function selectFilterMode(mode) {
  if (editId.value) return
  form.filterMode = mode
  if (mode === 'NONE') form.filterTags.splice(0)
}
function toggleFilterTag(tag) {
  if (editId.value || form.filterMode === 'NONE') return
  const index = form.filterTags.indexOf(tag)
  index >= 0 ? form.filterTags.splice(index, 1) : form.filterTags.push(tag)
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
  const index = form.trackIds.indexOf(track.id)
  if (index >= 0) {
    form.trackIds.splice(index, 1)
    selectedTracks.value.splice(index, 1)
    return
  }
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
function onPrivacyChange(event) { form.visibility = event.detail.value ? 'PRIVATE' : 'PUBLIC' }

async function onCreate() {
  if (submitting.value) return
  if (!canCreate.value) {
    const missing = !form.name.trim() ? 'Name your zone' : !form.scene ? 'Choose a scene' : 'Choose 3 tracks and review settings'
    uni.showToast({ title: missing, icon: 'none' })
    return
  }
  submitting.value = true
  try {
    if (editId.value) {
      await updateZone(editId.value, { name: form.name.trim(), scene: form.scene })
      uni.navigateBack()
      return
    }
    const payload = { name: form.name.trim(), scene: form.scene, filterMode: form.filterMode, filterTags: form.filterTags, visibility: form.visibility, password: form.password || null, trackIds: form.trackIds }
    const zone = customCoverPath.value ? await createZoneWithCover(payload, customCoverPath.value) : await createZone(payload)
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
  uni.switchTab({ url: '/pages/index/index', fail: () => uni.reLaunch({ url: '/pages/index/index' }) })
}
</script>

<style lang="scss" scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: radial-gradient(circle at 8% 7%, rgba(29,78,216,.07), transparent 28%), $sz-bg;
}
.nav {
  display: flex; align-items: center; justify-content: space-between; flex: 0 0 auto;
  padding: 12rpx 36rpx 16rpx; background: rgba(247,249,252,.88); backdrop-filter: blur(28px) saturate(120%);
  &__back { width: 64rpx; font-size: 58rpx; line-height: 1; color: $sz-text; }
  &__title { font-size: 31rpx; font-weight: 700; letter-spacing: -.5rpx; }
}
.progress {
  flex: 0 0 auto; padding: 18rpx 40rpx 22rpx;
  &__copy { display: flex; align-items: baseline; justify-content: space-between; }
  &__eyebrow { color: $sz-brand; font-size: 18rpx; font-weight: 700; letter-spacing: 2rpx; }
  &__title { color: $sz-text-secondary; font-size: 22rpx; font-weight: 600; }
  &__steps { display: flex; gap: 10rpx; margin-top: 14rpx; }
  &__step { flex: 1; height: 7rpx; border-radius: 999rpx; background: rgba(20,25,34,.1); transition: background-color .2s ease, transform .2s ease; }
  &__step--complete { background: rgba(29,78,216,.34); }
  &__step--current { background: $sz-brand; transform: scaleY(1.2); }
}
.wizard { flex: 1; width: 100%; height: 0; min-height: 0; &__item { height: 100%; } }
.step-scroll { width: 100%; height: 100%; }
.step-content { padding: 18rpx 40rpx 32rpx; }
.step-content--tracks { padding-top: 6rpx; }
.step-spacer { height: 42rpx; }
.step-intro {
  position: relative; padding: 8rpx 0 32rpx;
  &--compact { padding-bottom: 22rpx; }
  &__number { position: absolute; right: 0; top: -8rpx; color: rgba(29,78,216,.08); font-size: 104rpx; font-weight: 800; line-height: 1; }
  &__title { position: relative; display: block; max-width: 82%; color: $sz-text; font-size: 46rpx; line-height: 1.15; font-weight: 780; letter-spacing: -1.5rpx; }
  &__description { position: relative; display: block; max-width: 92%; margin-top: 14rpx; color: $sz-text-secondary; font-size: 23rpx; line-height: 1.55; }
}
.name-card { padding: 34rpx 30rpx 24rpx; border: 1rpx solid rgba(255,255,255,.82); border-radius: 32rpx; background: rgba(255,255,255,.76); box-shadow: 0 20rpx 55rpx rgba(30,55,84,.1); }
.name-card__footer { display: flex; justify-content: space-between; margin-top: 14rpx; color: $sz-text-tertiary; font-size: 18rpx; }
.name-card__ready { color: $sz-brand; font-weight: 600; }
.field-label { color: $sz-text-tertiary; font-size: 18rpx; font-weight: 700; letter-spacing: 2rpx; }
.name-input { height: 94rpx; border-bottom: 2rpx solid $sz-text; color: $sz-text; font-size: 30rpx; font-weight: 600; }
.placeholder { color: $sz-text-tertiary; font-weight: 400; }
.mini-preview { margin-top: 42rpx; }
.mini-preview__label { color: $sz-text-tertiary; font-size: 18rpx; font-weight: 700; letter-spacing: 2rpx; }
.mini-preview__card { display: flex; align-items: center; gap: 20rpx; margin-top: 14rpx; padding: 22rpx; border-radius: 26rpx; background: rgba(255,255,255,.6); }
.mini-preview__mark { width: 72rpx; height: 72rpx; display: flex; align-items: center; justify-content: center; border-radius: 22rpx; background: $sz-brand; color: #fff; font-size: 20rpx; font-weight: 800; }
.mini-preview__copy { display: flex; flex-direction: column; gap: 5rpx; min-width: 0; }
.mini-preview__name { color: $sz-text; font-size: 26rpx; font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mini-preview__meta { color: $sz-text-tertiary; font-size: 20rpx; }
.cover-picker {
  position: relative; width: 100%; height: 520rpx; overflow: hidden; border-radius: 42rpx; background: linear-gradient(145deg, #9DADBF, #D7DEE6); box-shadow: 0 28rpx 70rpx rgba(30,55,84,.15);
  &__image { width: 100%; height: 100%; }
  &__placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10rpx; color: rgba(255,255,255,.86); font-size: 22rpx; background: linear-gradient(145deg, rgba(255,255,255,.2), rgba(12,27,44,.15)); }
  &__symbol { font-size: 78rpx; font-weight: 300; }
  &__glass { position: absolute; right: 20rpx; bottom: 20rpx; left: 20rpx; display: flex; align-items: center; justify-content: space-between; padding: 20rpx 22rpx; border: 1rpx solid rgba(255,255,255,.56); border-radius: 22rpx; color: #fff; background: rgba(20,24,32,.3); backdrop-filter: blur(20px) saturate(120%); font-size: 19rpx; font-weight: 650; }
}
.cover-actions { display: flex; gap: 12rpx; margin-top: 20rpx; }
.cover-actions button { margin: 0; padding: 13rpx 22rpx; border-radius: 999rpx; font-size: 22rpx; line-height: 1.5; }
.cover-actions button::after { border: none; }
.cover-actions__primary { flex: 1; background: $sz-control; color: #fff; }
.cover-actions__remove { color: $sz-text-secondary; background: rgba(0,0,0,.055); }
.optional-note { display: flex; gap: 10rpx; margin-top: 18rpx; color: $sz-text-tertiary; font-size: 19rpx; line-height: 1.45; }
.optional-note__icon { color: $sz-brand; }
.quick-scenes { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12rpx; }
.quick-scene { min-height: 112rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6rpx; border: 1rpx solid transparent; border-radius: 26rpx; color: $sz-text-secondary; background: rgba(0,0,0,.045); font-size: 21rpx; font-weight: 600; transition: transform .16s ease, background-color .16s ease; }
.quick-scene:active { transform: scale(.96); }
.quick-scene--active { border-color: $sz-brand; background: $sz-brand; color: #fff; box-shadow: 0 14rpx 30rpx rgba(29,78,216,.2); }
.quick-scene__icon { font-size: 30rpx; line-height: 1; }
.browse-scenes { display: flex; align-items: center; gap: 18rpx; margin-top: 18rpx; padding: 22rpx; border: 1rpx solid rgba(29,78,216,.12); border-radius: 26rpx; background: rgba(255,255,255,.74); box-shadow: 0 14rpx 34rpx rgba(30,55,84,.07); }
.browse-scenes__icon { width: 54rpx; height: 54rpx; display: flex; align-items: center; justify-content: center; border-radius: 18rpx; background: $sz-control-soft; color: $sz-brand; font-size: 29rpx; }
.browse-scenes__copy { flex: 1; display: flex; flex-direction: column; gap: 3rpx; }
.browse-scenes__title { color: $sz-text; font-size: 24rpx; font-weight: 700; }
.browse-scenes__sub { color: $sz-text-tertiary; font-size: 19rpx; }
.browse-scenes__arrow { color: $sz-text-tertiary; font-size: 38rpx; }
.scene-selection { display: flex; align-items: center; gap: 18rpx; margin-top: 24rpx; padding: 20rpx 22rpx; border-radius: 26rpx; background: rgba(29,78,216,.08); }
.scene-selection__icon { width: 62rpx; height: 62rpx; display: flex; align-items: center; justify-content: center; border-radius: 20rpx; background: #fff; color: $sz-brand; font-size: 30rpx; }
.scene-selection__copy { flex: 1; display: flex; flex-direction: column; gap: 3rpx; }
.scene-selection__label { color: $sz-brand; font-size: 16rpx; font-weight: 750; letter-spacing: 1.6rpx; }
.scene-selection__name { color: $sz-text; font-size: 26rpx; font-weight: 700; }
.scene-selection__check { width: 42rpx; height: 42rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $sz-brand; color: #fff; font-size: 22rpx; }
.required-hint { display: block; margin-top: 15rpx; color: #a45151; font-size: 20rpx; }
.track-progress { margin-bottom: 20rpx; }
.track-progress__copy { display: flex; justify-content: space-between; color: $sz-text-secondary; font-size: 18rpx; font-weight: 700; letter-spacing: 1.5rpx; }
.track-progress__bar { height: 8rpx; margin-top: 10rpx; overflow: hidden; border-radius: 999rpx; background: rgba(0,0,0,.08); }
.track-progress__bar view { height: 100%; border-radius: 999rpx; background: $sz-brand; transition: width .2s ease; }
.selected-tracks { margin-bottom: 20rpx; padding: 0 16rpx; border-radius: 28rpx; background: rgba(29,78,216,.07); }
.track-search { display: flex; align-items: center; gap: 12rpx; height: 76rpx; padding: 0 22rpx; border: 1rpx solid $sz-control-border; border-radius: 24rpx; background: rgba(255,255,255,.78); }
.track-search__icon { color: $sz-brand; font-size: 29rpx; }
.track-search__input { flex: 1; font-size: 23rpx; }
.track-results { margin-top: 14rpx; }
.track-row { display: flex; align-items: center; gap: 16rpx; min-height: 108rpx; padding: 14rpx 4rpx; border-bottom: 1rpx solid rgba(0,0,0,.07); }
.track-row:last-child { border-bottom: 0; }
.track-row--conflict { opacity: .52; }
.track-row__index { width: 26rpx; color: $sz-brand; font-size: 20rpx; font-weight: 700; text-align: center; }
.track-row__cover { width: 82rpx; height: 82rpx; flex: 0 0 82rpx; display: flex; align-items: center; justify-content: center; overflow: hidden; border-radius: 18rpx; color: #fff; }
.track-row__cover-image { width: 100%; height: 100%; }
.track-row__info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3rpx; }
.track-row__title, .track-row__artist, .track-row__blocked { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.track-row__title { color: $sz-text; font-size: 23rpx; font-weight: 650; }
.track-row__artist { color: $sz-text-secondary; font-size: 19rpx; }
.track-row__blocked { color: #a45151; font-size: 17rpx; }
.track-row__action { width: 48rpx; height: 48rpx; flex: 0 0 48rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: $sz-control; color: #fff; font-size: 27rpx; }
.track-row__action--remove { background: rgba(0,0,0,.07); color: $sz-text-secondary; }
.track-empty { display: block; padding: 42rpx 0; color: $sz-text-tertiary; font-size: 21rpx; text-align: center; }
.setting-card { margin-bottom: 18rpx; padding: 26rpx; border: 1rpx solid rgba(255,255,255,.78); border-radius: 30rpx; background: rgba(255,255,255,.76); box-shadow: 0 14rpx 34rpx rgba(30,55,84,.07); }
.setting-card__heading { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.setting-card__heading > view { display: flex; flex-direction: column; gap: 4rpx; }
.setting-card__title { color: $sz-text; font-size: 25rpx; font-weight: 700; }
.setting-card__sub { color: $sz-text-tertiary; font-size: 18rpx; }
.setting-card__lock { padding: 6rpx 12rpx; border-radius: 999rpx; color: $sz-text-tertiary; background: rgba(0,0,0,.05); font-size: 16rpx; }
.mode-switch { display: flex; gap: 0; margin-top: 22rpx; padding: 6rpx; border-radius: 20rpx; background: rgba(0,0,0,.055); }
.mode-switch__option { flex: 1; padding: 15rpx 0; border-radius: 15rpx; color: $sz-text-secondary; font-size: 20rpx; text-align: center; }
.mode-switch__option--active { background: #fff; color: $sz-text; font-weight: 650; box-shadow: 0 3rpx 10rpx rgba(0,0,0,.07); }
.filter-groups { margin-top: 24rpx; }
.tag-group { margin-bottom: 20rpx; }
.tag-group__name { display: block; margin-bottom: 9rpx; color: $sz-text-secondary; font-size: 18rpx; font-weight: 650; }
.tag-group__scroll { width: 100%; white-space: nowrap; }
.tag-group__row { display: inline-flex; gap: 8rpx; padding-right: 20rpx; }
.tag { padding: 9rpx 17rpx; border: 1rpx solid transparent; border-radius: 999rpx; color: $sz-text-secondary; background: rgba(0,0,0,.05); font-size: 19rpx; }
.tag--active { border-color: $sz-control; background: $sz-control; color: #fff; }
.tag--disabled { opacity: .55; }
.password-input { height: 72rpx; margin-top: 20rpx; padding: 0 18rpx; border-radius: 18rpx; background: rgba(0,0,0,.045); font-size: 21rpx; }
.review-card { padding: 26rpx; border-radius: 30rpx; background: #1d2026; color: #fff; }
.review-card__eyebrow { display: block; margin-bottom: 16rpx; color: rgba(255,255,255,.54); font-size: 17rpx; font-weight: 700; letter-spacing: 2rpx; }
.review-card__row { display: flex; justify-content: space-between; gap: 24rpx; padding: 11rpx 0; border-bottom: 1rpx solid rgba(255,255,255,.1); font-size: 20rpx; }
.review-card__row:last-child { border-bottom: 0; }
.review-card__row text:first-child { color: rgba(255,255,255,.58); }
.review-card__row text:last-child { max-width: 62%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 650; }
.wizard-actions { flex: 0 0 auto; display: grid; grid-template-columns: 150rpx 1fr; gap: 14rpx; padding: 18rpx 40rpx calc(22rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid rgba(0,0,0,.055); background: rgba(255,255,255,.86); backdrop-filter: blur(28px) saturate(120%); }
.wizard-actions button { margin: 0; border-radius: 999rpx; font-size: 24rpx; line-height: 1.5; padding: 16rpx 18rpx; }
.wizard-actions button::after { border: none; }
.wizard-actions__secondary { color: $sz-text-secondary; background: rgba(0,0,0,.055); }
.wizard-actions__primary { color: $sz-text-tertiary; background: rgba(0,0,0,.09); font-weight: 650; }
.wizard-actions__primary--ready { color: #fff; background: $sz-brand; box-shadow: 0 14rpx 30rpx rgba(29,78,216,.22); }
.scene-picker-mask { position: fixed; inset: 0; z-index: 80; display: flex; align-items: flex-end; background: rgba(20,25,34,.22); }
.scene-picker { width: 100%; height: 79vh; display: flex; flex-direction: column; padding: 12rpx 32rpx calc(22rpx + env(safe-area-inset-bottom)); border-radius: 42rpx 42rpx 0 0; background: rgba(249,250,252,.98); box-shadow: 0 -26rpx 70rpx rgba(20,31,50,.18); backdrop-filter: blur(28px); }
.scene-picker__handle { width: 64rpx; height: 7rpx; margin: 0 auto 10rpx; border-radius: 999rpx; background: rgba(0,0,0,.15); }
.scene-picker__head { display: grid; grid-template-columns: 70rpx 1fr 70rpx; align-items: center; min-height: 72rpx; }
.scene-picker__close { font-size: 40rpx; color: $sz-text-secondary; }
.scene-picker__title { color: $sz-text; font-size: 28rpx; font-weight: 720; text-align: center; }
.scene-picker__done { color: $sz-brand; font-size: 22rpx; font-weight: 700; text-align: right; }
.scene-search { display: flex; align-items: center; gap: 12rpx; height: 78rpx; padding: 0 22rpx; border-radius: 24rpx; background: #eceff4; }
.scene-search__icon { color: $sz-text-tertiary; font-size: 30rpx; }
.scene-search__input { flex: 1; font-size: 23rpx; }
.scene-tabs { flex: 0 0 auto; width: 100%; white-space: nowrap; margin: 18rpx 0 16rpx; }
.scene-tabs__row { display: inline-flex; gap: 9rpx; }
.scene-tab { padding: 11rpx 20rpx; border: 1rpx solid rgba(0,0,0,.08); border-radius: 999rpx; color: $sz-text-secondary; background: #fff; font-size: 19rpx; font-weight: 600; }
.scene-tab--active { border-color: #1d2026; color: #fff; background: #1d2026; }
.scene-picker__list { flex: 1; min-height: 0; height: 0; }
.scene-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10rpx; }
.scene-option { min-height: 116rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 7rpx; border: 1rpx solid rgba(0,0,0,.04); border-radius: 24rpx; color: $sz-text-secondary; background: #fff; box-shadow: 0 8rpx 22rpx rgba(30,55,84,.05); font-size: 19rpx; font-weight: 650; }
.scene-option__icon { font-size: 28rpx; }
.scene-option--active { border-color: $sz-brand; color: #fff; background: $sz-brand; box-shadow: 0 12rpx 28rpx rgba(29,78,216,.22); }
.scene-empty { display: flex; flex-direction: column; align-items: center; gap: 7rpx; padding: 60rpx 20rpx 40rpx; color: $sz-text-tertiary; font-size: 20rpx; text-align: center; }
.custom-scene { margin-top: 22rpx; padding: 24rpx; border-radius: 28rpx; background: $sz-control-soft; }
.custom-scene__label { color: $sz-brand; font-size: 16rpx; font-weight: 750; letter-spacing: 1.8rpx; }
.custom-scene__form { display: grid; grid-template-columns: 1fr 120rpx; gap: 10rpx; margin-top: 12rpx; }
.custom-scene__input { min-width: 0; height: 70rpx; padding: 0 18rpx; border: 1rpx solid rgba(29,78,216,.16); border-radius: 18rpx; background: #fff; font-size: 21rpx; }
.custom-scene__button { margin: 0; padding: 0; border-radius: 18rpx; color: #fff; background: $sz-brand; font-size: 20rpx; font-weight: 700; line-height: 70rpx; }
.custom-scene__button::after { border: none; }
.custom-scene__hint { display: block; margin-top: 10rpx; color: $sz-text-tertiary; font-size: 17rpx; line-height: 1.45; }
</style>
