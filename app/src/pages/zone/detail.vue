<template>
  <view v-if="!zone" class="entry-state">
    <text>{{ entryMessage }}</text>
    <template v-if="needsPassword">
      <input v-model="password" password maxlength="32" placeholder="Enter private zone password" />
      <button :disabled="joining" @click="enterZone">Enter Zone</button>
    </template>
    <button v-if="!joining" @click="goBack">Back to Home</button>
  </view>
  <view v-else class="detail">
    <view v-if="glowing" class="edge-glow" />
    <!-- 顶部毛玻璃固定栏：返回 / 域名+在线人数 / 更多（决议 D9） -->
    <view class="nav sz-glass" :style="{ paddingTop: statusBarHeight + 'px' }">
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
      <view v-if="zone.nowPlaying" class="now-playing">
        <view class="now-playing__cover" :style="{ backgroundColor: zone.coverColor }">
          <image v-if="zone.nowPlaying.coverUrl" class="now-playing__cover-image" :src="zone.nowPlaying.coverUrl" mode="aspectFill" />
          <text v-else class="now-playing__note">♪</text>
        </view>
        <view class="now-playing__scrim" />
        <view class="now-playing__heart sz-glass-clear" :class="{ collected }" @click="onCollect">{{ collected ? '♥' : '♡' }}</view>
        <view v-if="likeEffect" class="like-effect" :class="'like-effect--' + effectTheme">
          <view v-if="effectTheme === 'study'" class="like-effect__ring like-effect__ring--one" />
          <view v-if="effectTheme === 'study'" class="like-effect__ring like-effect__ring--two" />
          <text v-if="effectTheme === 'fitness'" class="like-effect__ecg">﹏⌁﹏</text>
          <text v-for="n in 5" v-if="effectTheme === 'travel' || effectTheme === 'jpop' || effectTheme === 'night'" :key="n" class="like-effect__particle" :style="{ '--delay': (n * 70) + 'ms', '--dx': ((n - 3) * 40) + 'rpx' }">{{ effectTheme === 'travel' ? '✈' : effectTheme === 'jpop' ? '✿' : '✦' }}</text>
          <text class="like-effect__heart">♥</text>
        </view>
        <view class="now-playing__info sz-glass-tinted" :style="{ backgroundColor: (zone.coverColor || '#8c9bab') + '38' }">
          <text class="now-playing__eyebrow">NOW PLAYING</text>
          <text class="now-playing__title">{{ zone.nowPlaying.title }}</text>
          <text class="now-playing__artist" @click="goUserHome(zone.nowPlaying.userId)">{{ zone.nowPlaying.artist }} · uploaded by @{{ zone.nowPlaying.by }}</text>
          <text v-if="zone.nowPlaying.attribution" class="now-playing__source">{{ zone.nowPlaying.attribution }}</text>
          <progress class="now-playing__progress" :percent="progress" stroke-width="2" activeColor="#ffffff" backgroundColor="rgba(255,255,255,0.25)" />
          <view class="now-playing__times"><text>{{ formatCooldown(Math.floor((zone.nowPlaying.durationSec || 0) * progress / 100)) }}</text><text>{{ formatCooldown(zone.nowPlaying.durationSec || 0) }}</text></view>
        </view>
      </view>
      <view v-else class="now-playing now-playing--idle">
        <text class="now-playing__idle-text">Waiting for the first track</text>
      </view>

      <view v-if="playback.message" class="playback-message">{{ playback.message }}</view>
      <button v-if="playback.needsGesture" class="audio-unlock" @click="unlockAudio">
        {{ playback.loading ? 'Start when ready' : 'Start synchronized playback' }}
      </button>
      <!-- 动态分享区：主界面 1-2 张，可横滑，点击进入动态详情（决议 D6/D9） -->
      <view v-if="zone.moments.length" class="section">
        <view class="section__header" @click="goMoments">
          <text class="section__title">Moments</text>
          <text class="section__more">See all →</text>
        </view>
        <scroll-view scroll-x enhanced :show-scrollbar="false" class="moment-scroll">
          <view class="moment-scroll__inner">
            <view
              v-for="m in zone.moments.slice(0, 2)"
              :key="m.id"
              class="moment-scroll__item"
              @click="goMoments"
            >
              <moment-card
                :moment="m"
                compact
                :own="m.userId === session.userId"
                @withdraw="onWithdraw"
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
          <text class="section__more">{{ zone.queue.length }} queued</text>
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

    <!-- 底部悬浮毛玻璃操作栏：上传歌曲 / 上传图片（冷却置灰+倒计时，决议 D4/D9） -->
    <view class="action-bar sz-glass">
      <button
        class="action-bar__btn"
        :class="{ 'action-bar__btn--disabled': cooldown > 0 }"
        :disabled="cooldown > 0"
        @click="onUploadSong"
      >
        <text>Upload Song</text>
        <text v-if="cooldown > 0" class="action-bar__time"> ({{ formatCooldown(cooldown) }})</text>
      </button>
      <button class="action-bar__btn action-bar__btn--primary" :disabled="!imageCandidate" @click="openImage">Upload Photo</button>
    </view>

    <!-- 两个半屏玻璃弹窗 -->
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
import { ref, computed } from 'vue'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { getZoneDetail, getCooldown, collectTrack, withdrawMoment, joinZone, leaveZone, getInvite, reportZone } from '@/api/mock.js'
import { session } from '@/api/session.js'
import { playback, syncPlayer, positionSeconds, unlockAudio } from '@/services/player.js'
import { attachZone, leaveCurrentZone } from '@/services/zone-session.js'
import QueueItem from '@/components/queue-item/queue-item.vue'
import MomentCard from '@/components/moment-card/moment-card.vue'
import UploadSongPopup from '@/components/upload-song-popup/upload-song-popup.vue'
import UploadImagePopup from '@/components/upload-image-popup/upload-image-popup.vue'

const zone = ref(null)
const showSongPopup = ref(false)
const showImagePopup = ref(false)
const cooldown = ref(0)
const progress = ref(0)
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
let zoneId = null
let inviteCode = null
let timer = null
let glowTimer = null
let likeTimer = null
let cooldownUntil = 0
let refreshing = false
let collectBusy = false
let unloaded = false

onLoad((option) => {
  zoneId = Number(option.id)
  inviteCode = option.inviteCode || null
  if (!Number.isInteger(zoneId) || zoneId <= 0) { entryMessage.value = '域链接无效'; return }
  enterZone()
  timer = setInterval(() => {
    cooldown.value = Math.max(0, Math.ceil((cooldownUntil - Date.now()) / 1000))
    if (zone.value?.nowPlaying) progress.value = positionSeconds() / zone.value.nowPlaying.durationSec * 100
  }, 1000)
})
onShow(() => { if (zone.value) refresh() })
onUnload(() => { unloaded = true; clearInterval(timer); clearTimeout(glowTimer); clearTimeout(likeTimer); leaveCurrentZone() })

async function enterZone() {
  if (joining.value) return
  joining.value = true
  try {
    const started = Date.now()
    const data = await joinZone(zoneId, { inviteCode, password: password.value || null })
    if (unloaded) { await leaveZone(zoneId); return }
    zone.value = data
    needsPassword.value = false
    syncPlayer(data, started)
    await refreshCooldown()
    if (unloaded) { await leaveZone(zoneId); return }
    attachZone(zoneId, { changed: refresh, ended: endSession, glow: showGlow })
  } catch (e) {
    needsPassword.value = [3006, 3007].includes(e.code)
    entryMessage.value = e.message
  } finally { joining.value = false }
}
async function refresh() {
  if (refreshing || !zone.value || unloaded) return
  refreshing = true
  try {
    const started = Date.now()
    const data = await getZoneDetail(zoneId)
    if (unloaded || !zone.value) return
    if (data.stateVersion >= zone.value.stateVersion) { zone.value = data; syncPlayer(data, started) }
    await refreshCooldown()
  } catch (e) {
    if ([1002, 1003, 2001, 3004].includes(e.code)) endSession(e.message)
  } finally { refreshing = false }
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
}
function showGlow() {
  glowing.value = false
  clearTimeout(glowTimer)
  glowing.value = true
  glowTimer = setTimeout(() => { glowing.value = false }, 1800)
}
function onUploadSong() { if (!cooldown.value) showSongPopup.value = true }
function openImage() { if (imageCandidate.value) showImagePopup.value = true }
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
  const options = ['退出域', '举报', ...(own ? ['编辑域信息'] : []), ...(own && zone.value.visibility === 'PRIVATE' ? ['复制邀请链接'] : [])]
  uni.showActionSheet({ itemList: options, success: async ({ tapIndex }) => {
    const action = options[tapIndex]
    if (action === '退出域') { await leaveCurrentZone(); goBack() }
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
async function onWithdraw(momentId) {
  try { await withdrawMoment(zoneId, momentId); await refresh(); toast('已撤回') } catch (e) { toast(e.message) }
}
function goMoments() { uni.navigateTo({ url: `/pages/zone/moments?id=${zoneId}` }) }
function goUserHome(userId) {
  if (!Number.isInteger(userId) || userId <= 0) return
  if (userId === session.userId) uni.switchTab({ url: '/pages/user/index' })
  else uni.navigateTo({ url: `/pages/user/home?userId=${userId}` })
}
function goBack() { uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) }) }
function toast(title) { uni.showToast({ title, icon: 'none' }) }
function formatCooldown(sec) { return `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}` }
</script>

<style lang="scss" scoped>
.entry-state { padding: 100rpx 40rpx; display: flex; flex-direction: column; gap: 32rpx; color: $sz-text-secondary; }
.playback-message { text-align: center; color: $sz-text-tertiary; font-size: $sz-font-xs; padding: 12rpx; }
.audio-unlock { font-size: $sz-font-sm; margin: 16rpx; border-radius: 999rpx; background: $sz-primary; color: #fff; }
.edge-glow { position: fixed; inset: 0; z-index: 200; pointer-events: none; box-shadow: inset 0 0 55rpx rgba(168,184,200,.65); animation: glow 1.8s ease-out; }
@keyframes glow { 0%, 100% { opacity: 0; } 30% { opacity: 1; } }

.detail {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: $sz-bg;

  &__body {
    flex: 1;
    min-height: 0;
    height: 0;
    padding: 0 32rpx;
    box-sizing: border-box;
  }
}

.filter-note {
  margin-top: 20rpx;
  color: $sz-text-secondary;
  font-size: $sz-font-xs;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 顶部毛玻璃固定栏：轻透，模糊下方内容 */
.nav {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20rpx;
  padding-left: 32rpx;
  padding-right: 32rpx;
  border-radius: 0;
  background: rgba(255,255,255,.55);
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
  height: 560rpx;
  margin: 26rpx 0 30rpx;
  border-radius: 48rpx;
  overflow: hidden;
  box-shadow: 0 24rpx 70rpx rgba(0,0,0,.14);

  &__cover {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  &__cover-image {
    width: 100%;
    height: 100%;
  }

  &__note {
    color: #ffffff;
    font-size: 120rpx;
  }

  &__scrim { position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,.5), rgba(0,0,0,.05) 65%); }
  &__info { position: absolute; bottom: 0; left: 0; right: 0; padding: 28rpx 30rpx 24rpx; color: #fff; }
  &__eyebrow { display: block; font-size: 18rpx; font-weight: 600; letter-spacing: 2rpx; color: rgba(255,255,255,.74); margin-bottom: 5rpx; }
  &__title {
    display: block;
    font-size: 38rpx;
    font-weight: 600;
    line-height: 1.25;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &__source {
    display: block;
    color: rgba(255,255,255,.56);
    font-size: 19rpx;
    margin-bottom: 14rpx;
  }

  &__artist {
    display: block;
    font-size: 24rpx;
    color: rgba(255,255,255,.75);
    margin: 5rpx 0 18rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__progress { width: 100%; }
  &__times { display: flex; justify-content: space-between; color: rgba(255,255,255,.6); font-size: 18rpx; margin-top: 3rpx; }

  &__heart {
    position: absolute;
    right: 28rpx;
    top: 28rpx;
    width: 70rpx;
    height: 70rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 36rpx;
    z-index: 2;
    &.collected { color: #e3c9cd; }
  }

  &--idle {
    display: flex; align-items: center; justify-content: center;
    background: linear-gradient(140deg, #a8b8c8, #c3b8d9);
  }

  &__idle-text {
    color: #fff;
    font-size: 26rpx;
  }
}

.like-effect {
  position: absolute; inset: 0; z-index: 3; pointer-events: none;
  display: flex; align-items: center; justify-content: center;
  color: #a8b8c8;
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
    font-size: 21rpx;
    color: $sz-text-secondary;
    text-decoration: underline;
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
  gap: 20rpx;
  padding: 22rpx 32rpx calc(22rpx + env(safe-area-inset-bottom));
  border-radius: 0;
  background: rgba(255,255,255,.58);

  &__btn {
    flex: 1;
    font-size: 25rpx;
    font-weight: 600;
    background-color: rgba(0, 0, 0, 0.06);
    color: $sz-text-secondary;
    border-radius: 999rpx;

    &::after {
      border: none;
    }

    &--primary {
      background-color: $sz-primary;
      color: #ffffff;
      font-weight: 500;
    }

    /* 冷却置灰（决议 D4） */
    &--disabled {
      opacity: 0.45;
      color: $sz-text-secondary;
    }
  }
  &__time { font-size: 20rpx; font-weight: 400; }
}
</style>
