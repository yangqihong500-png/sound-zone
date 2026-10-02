<template>
  <view v-if="!zone" class="entry-state">
    <text>{{ entryMessage }}</text>
    <template v-if="needsPassword">
      <input v-model="password" password maxlength="32" placeholder="Enter private zone password" />
      <button class="sz-btn-primary" :disabled="joining" @click="enterZone">Enter Zone</button>
    </template>
    <button v-if="!joining" class="sz-btn-secondary" @click="goBack">Back to Home</button>
  </view>
  <view v-else class="detail" :style="detailThemeStyle">
    <view v-if="glowing" class="edge-glow">
      <view class="edge-glow__ring edge-glow__ring--outer" />
      <view class="edge-glow__ring edge-glow__ring--inner" />
    </view>
    <view class="detail__ambience">
      <image
        v-if="zone.nowPlaying?.coverUrl"
        class="detail__ambience-image"
        :src="zone.nowPlaying.coverUrl"
        mode="aspectFill"
      />
      <view class="detail__ambience-wash" />
    </view>
    <!-- 顶部毛玻璃固定栏：返回 / 域名+在线人数 / 更多（决议 D9） -->
    <view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <view class="nav__title">
        <text class="nav__name">{{ zone.name }}</text>
        <text class="nav__listeners">{{ zone.listeners }} listening now</text>
      </view>
      <view class="nav__more" @click="onMore">···</view>
    </view>

    <scroll-view class="detail__body" scroll-y>
      <view v-if="zone.filterTags?.length" class="filter-note">
        {{ zone.filterMode === 'BAN' ? 'Blocked tags' : 'Allowed tags' }}: {{ [...zone.filterTags].join(' · ') }}
      </view>
      <view v-if="zone.nowPlaying" class="now-playing now-playing--active">
        <view class="now-playing__cover" :style="{ backgroundColor: zone.coverColor }">
          <image v-if="zone.nowPlaying.coverUrl" class="now-playing__cover-image" :src="zone.nowPlaying.coverUrl" mode="aspectFill" />
          <text v-else class="now-playing__note">♪</text>
        </view>
        <view class="now-playing__heart" :class="{ collected }" @click="onCollect">{{ collected ? '♥' : '♡' }}</view>
        <view v-if="likeEffect" class="like-effect" :class="'like-effect--' + effectTheme">
          <view v-if="effectTheme === 'study'" class="like-effect__ring like-effect__ring--one" />
          <view v-if="effectTheme === 'study'" class="like-effect__ring like-effect__ring--two" />
          <text v-if="effectTheme === 'fitness'" class="like-effect__ecg">﹏⌁﹏</text>
          <text v-for="n in 5" v-if="effectTheme === 'travel' || effectTheme === 'jpop' || effectTheme === 'night'" :key="n" class="like-effect__particle" :style="{ '--delay': (n * 70) + 'ms', '--dx': ((n - 3) * 40) + 'rpx' }">{{ effectTheme === 'travel' ? '✈' : effectTheme === 'jpop' ? '✿' : '✦' }}</text>
          <text class="like-effect__heart">♥</text>
        </view>
        <view class="now-playing__info">
          <text class="now-playing__eyebrow">正在同步播放</text>
          <text class="now-playing__session">本次已共听 {{ formatSessionDuration(sessionListeningSeconds) }}</text>
          <text class="now-playing__title">{{ zone.nowPlaying.title }}</text>
          <text class="now-playing__artist" @click="goUserHome(zone.nowPlaying.userId)">{{ zone.nowPlaying.artist }} · uploaded by @{{ zone.nowPlaying.by }}</text>
          <text v-if="zone.nowPlaying.attribution" class="now-playing__source">{{ zone.nowPlaying.attribution }}</text>
          <progress class="now-playing__progress" :percent="progress" stroke-width="2" activeColor="#1C1C1E" backgroundColor="rgba(0,0,0,0.12)" />
          <view class="now-playing__times"><text>{{ formatCooldown(Math.floor((zone.nowPlaying.durationSec || 0) * progress / 100)) }}</text><text>{{ formatCooldown(zone.nowPlaying.durationSec || 0) }}</text></view>
        </view>
      </view>
      <view v-else class="now-playing now-playing--idle">
        <text class="now-playing__idle-text">Waiting for the first track</text>
      </view>

      <view v-if="playback.message" class="playback-message">{{ playback.message }}</view>
      <button v-if="playback.needsGesture" class="audio-unlock" @click="unlockAudio">
        点击开始同步播放
      </button>
      <!-- 动态分享区：主界面 1-2 张，可横滑，点击进入动态详情（决议 D6/D9） -->
      <view v-if="zone.moments.length" class="section">
        <view class="section__header" @click="goMoments">
          <text class="section__title">Moments</text>
          <text class="section__more section__more--action">See all →</text>
        </view>
        <scroll-view scroll-x enhanced :show-scrollbar="false" class="moment-scroll">
          <view class="moment-scroll__inner">
            <view
              v-for="m in zone.moments.slice(0, 2)"
              :key="m.id"
              class="moment-scroll__item"
              @click="goMoment(m.id)"
            >
              <moment-card
                :moment="m"
                compact
                @user="goUserHome"
              />
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 歌单列表区：FIFO 按上传顺序，当前播放高亮（决议 D3/D9） -->
      <view class="section">
        <view class="section__header">
          <text class="section__title">Up Next</text>
          <text class="section__more section__more--status">{{ zone.queue.length }} queued</text>
        </view>
        <view class="sz-card queue-card">
          <queue-item
            v-if="zone.nowPlaying"
            :item="zone.nowPlaying"
            :playing="true"
            @user="goUserHome"
          />
          <queue-item
            v-for="(song, i) in zone.queue"
            :key="song.itemId"
            :item="song"
            :rank="i + 1"
            @user="goUserHome"
          />
          <view v-if="!zone.queue.length && !zone.nowPlaying" class="queue-card__empty">
            The queue is empty
          </view>
        </view>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>

    <!-- 底部悬浮毛玻璃操作栏：上传歌曲；上传成功后可选附图（冷却置灰+倒计时，决议 D4/D9） -->
    <view class="action-bar sz-glass">
      <button
        class="action-bar__btn sz-btn-primary"
        :class="{ 'action-bar__btn--disabled': cooldown > 0, 'sz-btn-primary--disabled': cooldown > 0 }"
        :disabled="cooldown > 0"
        @click="onUploadSong"
      >
        <text>Upload Song</text>
        <text v-if="cooldown > 0" class="action-bar__time"> ({{ formatCooldown(cooldown) }})</text>
      </button>
    </view>

    <!-- 上传歌曲弹窗；成功后顺序打开可选附图弹窗 -->
    <upload-song-popup
      :visible="showSongPopup"
      :cooldown="cooldown"
      :zone-id="zone.id"
      @close="showSongPopup = false"
      @uploaded="onSongUploaded"
      @toast="toast"
    />
    <upload-image-popup
      :visible="showImagePopup"
      :bind-track="imageCandidate?.title || ''"
      :queue-item-id="imageCandidate?.itemId || null"
      :zone-id="zone.id"
      @close="showImagePopup = false"
      @uploaded="onImageUploaded"
      @toast="toast"
    />
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getCooldown, collectTrack, joinZone, leaveZone, getInvite, reportZone } from '@/api/mock.js'
import { session } from '@/api/session.js'
import { playback, syncPlayer, positionSeconds, unlockAudio } from '@/services/player.js'
import {
  activeZoneId,
  attachZone,
  detachZoneView,
  isZoneSuspended,
  leaveCurrentZone,
  refreshActiveZone,
  resumeZone,
  suspendCurrentZone,
} from '@/services/zone-session.js'
import QueueItem from '@/components/queue-item/queue-item.vue'
import MomentCard from '@/components/moment-card/moment-card.vue'
import UploadSongPopup from '@/components/upload-song-popup/upload-song-popup.vue'
import UploadImagePopup from '@/components/upload-image-popup/upload-image-popup.vue'

const zone = ref(null)
const showSongPopup = ref(false)
const showImagePopup = ref(false)
const cooldown = ref(0)
const progress = ref(0)
const sessionListeningSeconds = ref(0)
const password = ref('')
const needsPassword = ref(false)
const joining = ref(false)
const entryMessage = ref('正在进入域…')
const glowing = ref(false)
const likeEffect = ref(false)
const effectTheme = computed(() => ({ 自习: 'study', 健身: 'fitness', 旅行: 'travel', 日系: 'jpop', 深夜: 'night' }[zone.value?.scene] || 'study'))
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const imageCandidate = computed(() => zone.value?.imageCandidate)
const collected = computed(() => !!zone.value?.nowPlaying?.collected)
const extractedCoverPalette = ref(null)
const LIGHT_PALETTES = {
  ice: { base: '#B9DCF7', soft: '#E2F1FC', glow: 'rgba(67, 141, 208, 0.42)' },
  mint: { base: '#BFE8DB', soft: '#E3F5EF', glow: 'rgba(75, 174, 143, 0.38)' },
  lavender: { base: '#D7C9F0', soft: '#EEE9FA', glow: 'rgba(135, 107, 204, 0.38)' },
  peach: { base: '#F5C7AD', soft: '#FBE7D9', glow: 'rgba(215, 119, 75, 0.36)' },
  cover: { base: '#F4F5F7', soft: '#FBFBFC', glow: 'rgba(255, 255, 255, 0.08)' },
  neutral: { base: '#C9DDED', soft: '#ECF3F9', glow: 'rgba(100, 153, 204, 0.36)' },
}
const detailThemeStyle = computed(() => {
  // 有封面时由强模糊图片自然提供综合色相；四个浅色家族仅负责无封面/加载失败回退。
  const palette = extractedCoverPalette.value || (zone.value?.nowPlaying?.coverUrl
    ? LIGHT_PALETTES.cover
    : paletteForColor(zone.value?.coverColor)
  )
  return {
    '--ambient-base': palette.base,
    '--ambient-soft': palette.soft,
    '--ambient-glow': palette.glow,
  }
})
watch(
  () => zone.value?.nowPlaying?.coverUrl,
  async (coverUrl) => {
    extractedCoverPalette.value = null
    if (!coverUrl) return
    const palette = await extractCoverPalette(coverUrl)
    if (zone.value?.nowPlaying?.coverUrl === coverUrl) extractedCoverPalette.value = palette
  },
)
let zoneId = null
let inviteCode = null
let returnHome = false
let timer = null
let glowTimer = null
let likeTimer = null
let cooldownUntil = 0
let refreshing = false
let collectBusy = false
let unloaded = false
let lastSessionTick = 0

onLoad((option) => {
  zoneId = Number(option.id)
  inviteCode = option.inviteCode || null
  returnHome = option.returnHome === '1'
  if (!Number.isInteger(zoneId) || zoneId <= 0) { entryMessage.value = '域链接无效'; return }
  if (activeZoneId() === zoneId) restoreZone()
  else enterZone()
  timer = setInterval(() => {
    const now = Date.now()
    cooldown.value = Math.max(0, Math.ceil((cooldownUntil - Date.now()) / 1000))
    if (zone.value?.nowPlaying) progress.value = positionSeconds() / zone.value.nowPlaying.durationSec * 100
    if (lastSessionTick && playback.playing && playback.itemId && playback.itemId === zone.value?.nowPlaying?.itemId) {
      sessionListeningSeconds.value += Math.min(2, Math.max(0, Math.floor((now - lastSessionTick) / 1000)))
    }
    lastSessionTick = now
  }, 1000)
})
onShow(() => { if (zone.value) refresh() })
onUnload(() => {
  unloaded = true
  clearInterval(timer)
  clearTimeout(glowTimer)
  clearTimeout(likeTimer)
  if (isZoneSuspended(zoneId)) detachZoneView()
  else leaveCurrentZone()
})

function sessionHandlers() {
  return { changed: onSessionChanged, ended: endSession, glow: showGlow }
}

function onSessionChanged(data) {
  applySnapshot(data)
  refreshCooldown().catch(() => {})
}

async function restoreZone() {
  joining.value = true
  try {
    const data = await resumeZone(zoneId, sessionHandlers())
    if (!data || unloaded) return
    applySnapshot(data)
    sessionListeningSeconds.value = 0
    lastSessionTick = Date.now()
    await refreshCooldown()
  } catch (e) {
    entryMessage.value = e.message
  } finally { joining.value = false }
}

async function enterZone() {
  if (joining.value) return
  joining.value = true
  try {
    // 同一客户端只保留一个活动域；先完整退出已悬挂域，再加入新域，避免新音源被旧会话清理。
    if (activeZoneId() && activeZoneId() !== zoneId) await leaveCurrentZone()
    const started = Date.now()
    const data = await joinZone(zoneId, { inviteCode, password: password.value || null })
    if (unloaded) { await leaveZone(zoneId); return }
    zone.value = data
    sessionListeningSeconds.value = 0
    lastSessionTick = Date.now()
    needsPassword.value = false
    syncPlayer(data, started)
    await refreshCooldown()
    if (unloaded) { await leaveZone(zoneId); return }
    attachZone(zoneId, sessionHandlers(), data)
  } catch (e) {
    needsPassword.value = [3006, 3007].includes(e.code)
    entryMessage.value = e.message
  } finally { joining.value = false }
}
async function refresh() {
  if (refreshing || !zone.value || unloaded) return
  refreshing = true
  try {
    const data = await refreshActiveZone()
    if (data) applySnapshot(data)
    await refreshCooldown()
  } catch (e) {
    if ([1002, 1003, 2001, 3004].includes(e.code)) endSession(e.message)
  } finally { refreshing = false }
}

function applySnapshot(data) {
  if (unloaded || !data) return
  if (!zone.value || data.stateVersion >= zone.value.stateVersion) zone.value = data
}
async function refreshCooldown() {
  const remaining = await getCooldown(zoneId)
  cooldownUntil = Date.now() + remaining * 1000
  cooldown.value = remaining
}
function endSession(message) {
  leaveCurrentZone()
  zone.value = null
  entryMessage.value = message
  needsPassword.value = false
  sessionListeningSeconds.value = 0
  lastSessionTick = 0
}
function showGlow() {
  glowing.value = false
  clearTimeout(glowTimer)
  glowing.value = true
  glowTimer = setTimeout(() => { glowing.value = false }, 1800)
}
function onUploadSong() { if (!cooldown.value) showSongPopup.value = true }
function formatSessionDuration(seconds = 0) {
  const minutes = Math.floor(Math.max(0, seconds) / 60)
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`
}
async function onSongUploaded(item) {
  if (!zone.value) return
  zone.value.imageCandidate = item
  showImagePopup.value = true
  await refresh()
}
async function onImageUploaded() { showImagePopup.value = false; await refresh() }
async function onCollect() {
  if (collectBusy || !zone.value?.nowPlaying) return
  collectBusy = true
  const playing = zone.value.nowPlaying
  try {
    const active = await collectTrack(zoneId, playing.itemId, !playing.collected)
    if (zone.value?.nowPlaying?.itemId === playing.itemId) zone.value.nowPlaying.collected = active
    if (active) {
      likeEffect.value = false
      clearTimeout(likeTimer)
      likeEffect.value = true
      likeTimer = setTimeout(() => { likeEffect.value = false }, 1500)
    }
  } catch (e) { toast(e.message) }
  finally { collectBusy = false }
}
function onMore() {
  const own = zone.value.hostId === session.userId
  const options = ['悬挂域并继续播放', '退出域', '举报', ...(own ? ['编辑域信息'] : []), ...(own && zone.value.visibility === 'PRIVATE' ? ['复制邀请链接'] : [])]
  uni.showActionSheet({ itemList: options, success: async ({ tapIndex }) => {
    const action = options[tapIndex]
    if (action === '悬挂域并继续播放') {
      if (!suspendCurrentZone(zone.value)) return
      toast('域已悬挂，可边浏览边听')
      goHome()
    }
    else if (action === '退出域') { await leaveCurrentZone(); goBack() }
    else if (action === '举报') uni.showModal({ title: '举报', editable: true, placeholderText: '请说明举报原因', success: async (res) => {
      if (!res.confirm) return
      try { await reportZone(zoneId, res.content); toast('举报已提交') } catch (e) { toast(e.message) }
    } })
    else if (action === '编辑域信息') uni.navigateTo({ url: `/pages/zone/create?id=${zoneId}` })
    else {
      try {
        const { inviteCode: code } = await getInvite(zoneId)
        const path = `/pages/zone/detail?id=${zoneId}&inviteCode=${encodeURIComponent(code)}`
        let base = import.meta.env.VITE_H5_SHARE_BASE || ''
        // #ifdef H5
        base = base || window.location.href.split('#')[0]
        // #endif
        if (!base) { toast('请配置 H5 分享地址后复制邀请链接'); return }
        uni.setClipboardData({ data: base + '#' + path })
      } catch (e) { toast(e.message) }
    }
  } })
}
function goMoments() { uni.navigateTo({ url: `/pages/zone/moments?id=${zoneId}` }) }
function goMoment(momentId) { uni.navigateTo({ url: `/pages/zone/moment-detail?id=${momentId}` }) }
function goUserHome(userId) {
  if (!Number.isInteger(userId) || userId <= 0) return
  if (userId === session.userId) uni.switchTab({ url: '/pages/user/index' })
  else uni.navigateTo({ url: `/pages/user/home?userId=${userId}` })
}
function goBack() {
  // 创建成功或外部直达详情时没有可靠的上一页，直接回 Home。
  if (returnHome || getCurrentPages().length <= 1) {
    goHome()
    return
  }
  uni.navigateBack({
    fail: goHome,
  })
}
function goHome() {
  uni.switchTab({
    url: '/pages/index/index',
    fail: () => uni.reLaunch({ url: '/pages/index/index' }),
  })
}
function toast(title) { uni.showToast({ title, icon: 'none' }) }
function formatCooldown(sec) { return `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}` }

function paletteForColor(value) {
  const rgb = parseHexColor(value)
  if (!rgb) return LIGHT_PALETTES.neutral
  const { hue, saturation } = rgbToHsl(rgb)
  if (saturation < 0.08) return LIGHT_PALETTES.neutral
  if (hue >= 70 && hue < 190) return LIGHT_PALETTES.mint
  if (hue >= 190 && hue < 250) return LIGHT_PALETTES.ice
  if (hue >= 250 && hue < 350) return LIGHT_PALETTES.lavender
  return LIGHT_PALETTES.peach
}

function parseHexColor(value) {
  const hex = String(value || '').trim().replace(/^#/, '')
  if (!/^[\da-f]{6}$/i.test(hex)) return null
  return {
    red: Number.parseInt(hex.slice(0, 2), 16) / 255,
    green: Number.parseInt(hex.slice(2, 4), 16) / 255,
    blue: Number.parseInt(hex.slice(4, 6), 16) / 255,
  }
}

function rgbToHsl({ red, green, blue }) {
  const max = Math.max(red, green, blue)
  const min = Math.min(red, green, blue)
  const delta = max - min
  const lightness = (max + min) / 2
  let hue = 0
  if (delta) {
    if (max === red) hue = 60 * (((green - blue) / delta) % 6)
    else if (max === green) hue = 60 * ((blue - red) / delta + 2)
    else hue = 60 * ((red - green) / delta + 4)
  }
  if (hue < 0) hue += 360
  const saturation = delta ? delta / (1 - Math.abs(2 * lightness - 1)) : 0
  return { hue, saturation, lightness }
}

async function extractCoverPalette(url) {
  // #ifdef H5
  return new Promise((resolve) => {
    const image = new window.Image()
    image.crossOrigin = 'anonymous'
    image.onload = () => {
      try {
        const canvas = document.createElement('canvas')
        canvas.width = 32
        canvas.height = 32
        const context = canvas.getContext('2d', { willReadFrequently: true })
        context.drawImage(image, 0, 0, 32, 32)
        resolve(paletteFromPixels(context.getImageData(0, 0, 32, 32).data))
      } catch { resolve(null) }
    }
    image.onerror = () => resolve(null)
    image.src = url
  })
  // #endif
  // #ifndef H5
  return null
  // #endif
}

function paletteFromPixels(pixels) {
  const buckets = Array.from({ length: 24 }, () => ({ weight: 0, hue: 0 }))
  for (let index = 0; index < pixels.length; index += 16) {
    if (pixels[index + 3] < 180) continue
    const rgb = {
      red: pixels[index] / 255,
      green: pixels[index + 1] / 255,
      blue: pixels[index + 2] / 255,
    }
    const { hue, saturation, lightness } = rgbToHsl(rgb)
    // 黑白灰不参与主色判断，避免人物、文字和黑底把彩色封面洗灰。
    if (saturation < 0.18 || lightness < 0.1 || lightness > 0.92) continue
    const weight = (0.45 + saturation) * (1 - Math.abs(lightness - 0.56) * 0.7)
    const bucket = buckets[Math.min(23, Math.floor(hue / 15))]
    bucket.weight += weight
    bucket.hue += hue * weight
  }
  const winner = buckets.reduce((best, item) => item.weight > best.weight ? item : best)
  if (winner.weight < 2) return null
  const hue = Math.round(winner.hue / winner.weight)
  return {
    base: `hsl(${hue}, 68%, 78%)`,
    soft: `hsl(${hue}, 52%, 90%)`,
    glow: `hsla(${hue}, 76%, 54%, 0.42)`,
  }
}
</script>

<style lang="scss" scoped>
.entry-state { padding: 100rpx 40rpx; display: flex; flex-direction: column; gap: 32rpx; color: $sz-text-secondary; }
.playback-message { text-align: center; color: $sz-text-tertiary; font-size: $sz-font-xs; padding: 12rpx; }
.audio-unlock {
  display: flex;
  width: fit-content;
  min-height: 0;
  margin: 0 auto 12rpx;
  padding: 4rpx 16rpx;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 999rpx;
  background: $sz-control-soft;
  box-shadow: none;
  color: $sz-control;
  font-size: $sz-font-xs;
  font-weight: 500;
  line-height: 1.6;
}
.audio-unlock::after { border: 0; }
.edge-glow {
  position: fixed;
  inset: 0;
  z-index: 200;
  overflow: hidden;
  pointer-events: none;
  background: radial-gradient(ellipse at center, transparent 56%, rgba(28,28,30,.04) 78%, rgba(135,148,164,.24) 100%);
  animation: edge-glow-veil 1.8s ease-out forwards;

  &__ring {
    position: absolute;
    border-style: solid;
    border-color: rgba(202,224,246,.9);
    border-radius: 42rpx;
    box-shadow: inset 0 0 48rpx rgba(28,28,30,.28), 0 0 30rpx rgba(170,181,194,.46);
    opacity: 0;
  }

  &__ring--outer {
    inset: -8rpx;
    border-width: 8rpx;
    animation: edge-glow-ring 1.35s cubic-bezier(.2,.7,.2,1) forwards;
  }

  &__ring--inner {
    inset: 22rpx;
    border-width: 3rpx;
    animation: edge-glow-ring 1.25s .12s cubic-bezier(.2,.7,.2,1) forwards;
  }
}
@keyframes edge-glow-veil { 0%, 100% { opacity: 0; } 18%, 45% { opacity: 1; } 30% { opacity: .62; } }
@keyframes edge-glow-ring {
  0% { opacity: 0; transform: scale(.985); }
  18% { opacity: .95; }
  36% { opacity: .38; }
  52% { opacity: .9; }
  100% { opacity: 0; transform: scale(1.035); }
}

.detail {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: $sz-bg;
  overflow: hidden;

  &__ambience {
    position: absolute;
    z-index: 0;
    top: 0;
    left: 0;
    right: 0;
    height: 820rpx;
    overflow: hidden;
    background: linear-gradient(155deg, var(--ambient-base), var(--ambient-soft));
    pointer-events: none;
  }

  &__ambience-image {
    position: absolute;
    top: -140rpx;
    left: -100rpx;
    width: calc(100% + 200rpx);
    height: 720rpx;
    opacity: .76;
    filter: blur(68rpx) saturate(1.38) brightness(1.2);
    transform: scale(1.18);
  }

  &__ambience-wash {
    position: absolute;
    inset: 0;
    background:
      radial-gradient(circle at 24% 22%, var(--ambient-glow), transparent 46%),
      linear-gradient(180deg, rgba(255,255,255,.16) 0%, rgba(255,255,255,.4) 58%, $sz-bg 100%);
  }

  &__body {
    position: relative;
    z-index: 1;
    flex: 1;
    min-height: 0;
    height: 0;
    padding: 0 32rpx;
    box-sizing: border-box;
  }
}

.filter-note {
  margin-top: 20rpx;
  padding: 7rpx 16rpx;
  border: 1rpx solid rgba(255,255,255,.72);
  border-radius: 999rpx;
  background: rgba(255,255,255,.68);
  color: #344765;
  font-size: $sz-font-xs;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 顶部毛玻璃固定栏：轻透，模糊下方内容 */
.nav {
  position: relative;
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20rpx;
  padding-left: 32rpx;
  padding-right: 32rpx;
  background: rgba(255,255,255,.16);
  backdrop-filter: blur(28px) saturate(115%);
  -webkit-backdrop-filter: blur(28px) saturate(115%);
  z-index: 5;

  &__back {
    font-size: 56rpx;
    line-height: 1;
    color: $sz-text;
    width: 60rpx;
  }

  &__title {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__name {
    font-size: 29rpx;
    font-weight: 600;
    color: #0b2348;
  }

  &__listeners {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }

  &__more {
    font-size: $sz-font-lg;
    color: $sz-text-secondary;
    width: 60rpx;
    text-align: right;
  }
}

.now-playing {
  position: relative;
  min-height: 332rpx;
  margin: 26rpx 0 30rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(255,255,255,.8);
  border-radius: 42rpx;
  overflow: hidden;
  box-shadow: 0 24rpx 60rpx rgba(67,104,145,.12);

  &--active {
    display: flex;
    align-items: center;
    gap: 24rpx;
    padding: 26rpx;
    background: rgba(255,255,255,.62);
    backdrop-filter: blur(28px) saturate(112%);
    -webkit-backdrop-filter: blur(28px) saturate(112%);
  }

  &__cover {
    width: 252rpx;
    height: 252rpx;
    flex: 0 0 252rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    border: 1rpx solid rgba(255,255,255,.78);
    border-radius: 30rpx;
    box-shadow: 0 16rpx 38rpx rgba(32,61,94,.16);
  }

  &__cover-image {
    width: 100%;
    height: 100%;
  }

  &__note {
    color: #0b2348;
    font-size: 120rpx;
  }

  &__info {
    min-width: 0;
    flex: 1;
    padding: 6rpx 4rpx;
    color: $sz-text;
  }
  &__eyebrow { display: block; font-size: 19rpx; font-weight: 600; color: $sz-text-secondary; margin-bottom: 8rpx; }
  &__session { display: block; margin: -3rpx 0 9rpx; color: rgba(11,35,72,.52); font-size: 18rpx; }
  &__title {
    display: block;
    padding-right: 52rpx;
    font-size: 32rpx;
    font-weight: 700;
    line-height: 1.25;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &__source {
    display: block;
    color: rgba(11,35,72,.48);
    font-size: 19rpx;
    margin-bottom: 16rpx;
  }

  &__artist {
    display: block;
    font-size: 22rpx;
    color: rgba(11,35,72,.72);
    margin: 6rpx 0 12rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__progress { width: 100%; }
  &__times { display: flex; justify-content: space-between; color: rgba(11,35,72,.5); font-size: 18rpx; margin-top: 3rpx; }

  &__heart {
    position: absolute;
    right: 24rpx;
    top: 24rpx;
    width: 56rpx;
    height: 56rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1rpx solid $sz-control-border;
    background: rgba(255,255,255,.68);
    color: $sz-control;
    font-size: 31rpx;
    z-index: 2;
    &.collected { color: #ffffff; background: $sz-control; }
  }

  &--idle {
    display: flex; align-items: center; justify-content: center;
    background: linear-gradient(140deg, var(--ambient-base), var(--ambient-soft));
  }

  &__idle-text {
    color: #0b2348;
    font-size: 26rpx;
  }
}

.like-effect {
  position: absolute; inset: 0; z-index: 3; pointer-events: none;
  display: flex; align-items: center; justify-content: center;
  color: $sz-control;
  &__heart { font-size: 60rpx; animation: like-pop 1.1s ease-out forwards; }
  &__ring { position: absolute; width: 110rpx; height: 110rpx; border-radius: 50%; border: 3rpx solid currentColor; animation: like-ring 1.1s ease-out forwards; }
  &__ring--two { animation-delay: .18s; }
  &__ecg { position: absolute; font-size: 80rpx; letter-spacing: 4rpx; animation: like-pulse .9s ease-in-out forwards; }
  &__particle { position: absolute; font-size: 38rpx; animation: like-drift 1.4s ease-out forwards; animation-delay: var(--delay); }
  &--fitness { color: #a9c4b5; }
  &--travel { color: #d9cfb8; }
  &--jpop { color: #e3c9cd; }
  &--night { color: #c3b8d9; }
  &--travel .like-effect__particle:nth-child(2n) { animation-direction: reverse; }
  &--night .like-effect__particle { font-size: 28rpx; }
}
@keyframes like-pop { 0% { opacity: 0; transform: scale(.4); } 35% { opacity: 1; transform: scale(1.2); } 100% { opacity: 0; transform: scale(.9); } }
@keyframes like-ring { 0% { opacity: .7; transform: scale(.4); } 100% { opacity: 0; transform: scale(2.2); } }
@keyframes like-pulse { 0%, 100% { opacity: 0; transform: scaleX(.6); } 50% { opacity: 1; transform: scaleX(1.2); } }
@keyframes like-drift { 0% { opacity: 1; transform: translate(0,0) rotate(0); } 100% { opacity: 0; transform: translate(var(--dx), -130rpx) rotate(35deg); } }

.section {
  margin-top: 34rpx;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    padding: 0 8rpx;
    margin-bottom: $sz-gap-sm;
  }

  &__title {
    font-size: 27rpx;
    font-weight: 600;
  }

  &__more {
    display: inline-flex;
    align-items: center;
    min-height: 44rpx;
    box-sizing: border-box;
    padding: 5rpx 16rpx;
    border: 1rpx solid $sz-control-border;
    border-radius: 999rpx;
    background: $sz-control-soft;
    color: $sz-control;
    font-size: 20rpx;
    font-weight: 600;
    line-height: 1;
    text-decoration: none;
  }
}

.moment-scroll {
  white-space: nowrap;

  &__inner {
    display: inline-flex;
    gap: 24rpx;
  }

  &__item {
    width: 230rpx;
    flex-shrink: 0;
  }
}

.queue-card {
  padding: 6rpx 12rpx;
  box-shadow: none;
  background: transparent;

  &__empty {
    text-align: center;
    color: $sz-text-tertiary;
    font-size: $sz-font-sm;
    padding: 40rpx 0;
  }
}

.bottom-spacer {
  height: 160rpx;
}

/* 底部悬浮毛玻璃操作栏 */
.action-bar {
  display: flex;
  flex-shrink: 0;
  padding: 22rpx 32rpx calc(22rpx + env(safe-area-inset-bottom));
  border-radius: 0;
  background: rgba(255,255,255,.86);

  &__btn {
    flex: 1;
    font-size: 25rpx;
    font-weight: 600;
    background-color: $sz-brand;
    color: #ffffff;
    border-radius: 999rpx;
    box-shadow: 0 14rpx 30rpx rgba(29,78,216,.22);

    &::after {
      border: none;
    }

    /* 冷却置灰（决议 D4） */
    &--disabled {
      background-color: rgba(0,0,0,.08);
      box-shadow: none;
      color: $sz-text-tertiary;
      opacity: 1;
    }
  }
  &__time { font-size: 20rpx; font-weight: 400; }
}
</style>
