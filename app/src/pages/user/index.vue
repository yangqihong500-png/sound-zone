<template>
  <view class="page">
    <view class="profile-card">
      <view class="profile-card__glow" :style="{ backgroundColor: user.avatarColor }" />
      <view class="profile-card__avatar" :style="{ backgroundColor: user.avatarColor }">
        <text>{{ (user.name || '?')[0].toUpperCase() }}</text>
      </view>
      <text class="profile-card__name">@{{ user.name }}</text>
      <text class="profile-card__slogan">someone is always listening with you</text>
      <view v-if="session.passwordAuth" class="profile-card__account" @click="onAccount">
        <text>{{ session.guest ? 'Guest · Log in or register' : 'Registered account · Log out' }}</text>
      </view>
      <text v-else class="profile-card__account-note">Guest demo · Account login requires HTTPS</text>
    </view>

    <view class="stats">
      <view v-for="stat in stats" :key="stat.label" class="stats__item">
        <text class="stats__value">{{ stat.value }}</text>
        <text class="stats__label">{{ stat.label }}</text>
      </view>
    </view>

    <view class="listening-card" @click="openListening">
      <view class="listening-card__icon">♪</view>
      <view class="listening-card__copy">
        <text class="listening-card__title">Listening Time</text>
        <text class="listening-card__caption">共听时长</text>
      </view>
      <text class="listening-card__value">{{ formatDuration(listening.totalSeconds) }}</text>
      <text class="listening-card__arrow">›</text>
    </view>

    <view class="menu">
      <view v-for="item in menus" :key="item.kind" class="menu__item" @click="onMenu(item)">
        <text class="menu__icon">{{ item.icon }}</text>
        <text class="menu__title">{{ item.title }}</text>
        <text class="menu__arrow">›</text>
      </view>
    </view>
    <suspended-zone-player />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getCurrentUser, getListeningSummary } from '@/api/mock.js'
import { session, logoutAccount } from '@/api/session.js'

const user = ref({ name: 'Loading…', avatarColor: '#a8b8c8', stats: { uploads: 0, likes: 0, moments: 0, following: 0 } })
const listening = ref({ totalSeconds: 0 })
const stats = computed(() => [
  { label: 'Uploads', value: formatStat(user.value.stats?.uploads) },
  { label: 'Likes', value: formatStat(user.value.stats?.likes) },
  { label: 'Shares', value: formatStat(user.value.stats?.moments) },
  { label: 'Following', value: formatStat(user.value.stats?.following) },
])
function formatStat(value = 0) {
  return value >= 1000 ? (value / 1000).toFixed(1).replace(/\.0$/, '') + 'k' : value
}
const menus = [
  { title: 'My Zones', kind: 'zones', icon: '◎' },
  { title: 'My Uploads', kind: 'uploads', icon: '♫' },
  { title: 'Following', kind: 'following', icon: '◇' },
  { title: 'Favorites', kind: 'collections', icon: '♡' },
]
onShow(async () => {
  try { user.value = await getCurrentUser() }
  catch (e) { user.value.name = 'Unavailable'; uni.showToast({ title: e.message, icon: 'none' }) }
  try { listening.value = await getListeningSummary() }
  catch { listening.value = { totalSeconds: 0 } }
})
function onMenu(item) { uni.navigateTo({ url: '/pages/user/library?kind=' + item.kind }) }
function openListening() { uni.navigateTo({ url: '/pages/user/listening' }) }
function formatDuration(seconds = 0) {
  const minutes = Math.floor(Math.max(0, seconds) / 60)
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours}h ${rest}m` : `${hours}h`
}
async function onAccount() {
  if (session.guest) {
    uni.navigateTo({ url: '/pages/auth/account' })
    return
  }
  uni.showModal({ title: 'Log out?', content: 'You can sign in again with your username and password.', success: async ({ confirm }) => {
    if (!confirm) return
    await logoutAccount()
    uni.navigateTo({ url: '/pages/auth/account' })
  } })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 32rpx 32rpx 90rpx; background: $sz-bg; }
.profile-card {
  position: relative; overflow: hidden; min-height: 360rpx; border-radius: 48rpx;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: linear-gradient(135deg, rgba(28,28,30,.06), rgba(255,255,255,.72)), #fff;
  box-shadow: $sz-shadow-soft;
}
.profile-card__glow { position: absolute; top: 40rpx; width: 180rpx; height: 180rpx; border-radius: 50%; filter: blur(32rpx); opacity: .45; }
.profile-card__avatar { position: relative; width: 140rpx; height: 140rpx; border: 4rpx solid rgba(255,255,255,.85); box-shadow: $sz-shadow-float; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 58rpx; font-weight: 600; }
.profile-card__name { position: relative; margin-top: 18rpx; font-size: 34rpx; font-weight: 600; color: $sz-text; }
.profile-card__slogan { position: relative; margin-top: 4rpx; font-size: 22rpx; font-style: italic; color: $sz-text-tertiary; }
.profile-card__account { position: relative; margin-top: 20rpx; padding: 10rpx 20rpx; border-radius: 999rpx; background: rgba(255,255,255,.62); border: 1rpx solid rgba(0,0,0,.08); color: $sz-text-secondary; font-size: 20rpx; }
.profile-card__account-note { position: relative; margin-top: 20rpx; color: $sz-text-tertiary; font-size: 19rpx; }
.stats { display: flex; margin-top: 24rpx; border-radius: 32rpx; background: #fff; box-shadow: $sz-shadow-soft; overflow: hidden; }
.stats__item { position: relative; flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: center; padding: 28rpx 0; }
.stats__item:not(:last-child)::after { content: ''; position: absolute; right: 0; top: 25%; height: 50%; width: 1rpx; background: rgba(0,0,0,.07); }
.stats__value { font-size: 34rpx; font-weight: 600; color: $sz-text; }
.stats__label { margin-top: 3rpx; color: $sz-text-tertiary; font-size: 19rpx; }
.listening-card { display: flex; align-items: center; gap: 20rpx; min-height: 112rpx; box-sizing: border-box; margin-top: 24rpx; padding: 20rpx 28rpx; border-radius: 32rpx; background: #fff; box-shadow: $sz-shadow-soft; }
.listening-card:active { transform: scale(.995); }
.listening-card__icon { width: 58rpx; height: 58rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; background: #1c1c1e; color: #fff; font-size: 30rpx; }
.listening-card__copy { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.listening-card__title { color: $sz-text; font-size: 27rpx; font-weight: 600; }
.listening-card__caption { margin-top: 2rpx; color: $sz-text-tertiary; font-size: 19rpx; }
.listening-card__value { flex-shrink: 0; color: $sz-text; font-size: 30rpx; font-weight: 600; }
.listening-card__arrow { flex-shrink: 0; color: $sz-text-tertiary; font-size: 38rpx; }
.menu { margin-top: 24rpx; padding: 0 30rpx; border-radius: 32rpx; background: #fff; box-shadow: $sz-shadow-soft; }
.menu__item { display: flex; align-items: center; gap: 24rpx; height: 100rpx; border-bottom: 1rpx solid rgba(0,0,0,.06); }
.menu__item:last-child { border-bottom: 0; }
.menu__icon { width: 35rpx; color: $sz-text-secondary; font-size: 34rpx; text-align: center; }
.menu__title { flex: 1; font-size: 27rpx; font-weight: 500; color: $sz-text; }
.menu__arrow { font-size: 38rpx; color: $sz-text-tertiary; }
</style>
