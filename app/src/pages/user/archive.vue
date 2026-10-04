<template>
  <view class="archive-page">
    <view class="archive-nav" :style="{ paddingTop: statusBarHeight + 'px' }"><view @click="goBack">‹</view><text>声音档案</text><view class="archive-nav__end" /></view>
    <view v-if="loading" class="archive-state">Loading…</view>
    <view v-else-if="error" class="archive-state">{{ error }}</view>
    <view v-else-if="user" class="paper">
      <view class="paper__masthead"><view><text class="paper__eyebrow">MUSIC SHARING / PUBLIC PROFILE</text><text class="paper__brand">SOUNDZONE<text class="paper__brand-dot">.</text></text><text class="paper__sub">LISTENER RADIO REPORT</text></view><view class="paper__number"><text>NO.</text><text class="paper__number-value">{{ serial }}</text><text>PUBLIC COPY</text></view></view>
      <view class="paper__identity"><view class="paper__portrait"><image v-if="avatarUrl" class="paper__portrait-image" :src="avatarUrl" mode="aspectFill" /><text v-else>{{ initial }}</text><text class="paper__portrait-label">PHOTO / AVATAR</text></view><view class="paper__fields"><view class="paper__field"><text>NAME</text><text>{{ user.name }}</text></view><view class="paper__field"><text>SOUND ID</text><text>SZ-{{ serial }}</text></view><view class="paper__field"><text>UPLOADS</text><text>{{ user.stats?.uploads || 0 }} TRACKS</text></view><view class="paper__field"><text>SHARES</text><text>{{ user.stats?.moments || 0 }} MOMENTS</text></view></view></view>
      <view class="paper__section"><view class="paper__section-heading"><text>SELF INTRODUCTION / 自我介绍</text><text>+</text></view><view class="paper__line-copy"><text>someone is always listening with you.</text><text>在 SoundZone，声音会留下相遇的痕迹。</text></view></view>
      <view class="paper__section"><view class="paper__section-heading"><text>MUSIC FOOTPRINT / 声音记录</text><text>PUBLIC</text></view><view class="paper__stat-row"><text>01</text><text>上传歌曲</text><text>{{ user.stats?.uploads || 0 }}</text></view><view class="paper__stat-row"><text>02</text><text>收到喜欢</text><text>{{ user.stats?.likes || 0 }}</text></view><view class="paper__stat-row"><text>03</text><text>分享瞬间</text><text>{{ user.stats?.moments || 0 }}</text></view></view>
      <view class="paper__section"><view class="paper__section-heading"><text>VISIBILITY / 可见范围</text><text>OPEN</text></view><view class="paper__note">这份档案只展示公开的用户 ID、头像和统计。私密域、收藏及关注名单不会在这里出现。</view></view>
      <view class="paper__signature"><view><text class="paper__signature-label">SIGNED BY LISTENER</text><text class="paper__signature-name">{{ user.name }}</text></view><view class="paper__seal"><text>SOUNDZONE</text><text>PUBLIC</text><text>PROFILE</text></view></view>
      <view class="paper__footer"><text>SoundZone Radio · SZ-{{ serial }}</text><text>PROFILE / PUBLIC</text></view>
    </view>
    <suspended-zone-player />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getUserProfile, resolveMediaUrl } from '@/api/mock.js'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const user = ref(null)
const loading = ref(true)
const error = ref('')
const serial = computed(() => String(user.value?.id || 0).padStart(3, '0'))
const initial = computed(() => (user.value?.name || '?').trim().charAt(0).toUpperCase() || '?')
const avatarUrl = computed(() => resolveMediaUrl(user.value?.avatarUrl))
onLoad(async (options) => {
  const userId = Number(options.userId)
  if (!options.userId || !Number.isInteger(userId) || userId <= 0) { loading.value = false; error.value = '无效的用户 ID'; return }
  try { user.value = await getUserProfile(userId) }
  catch (e) { error.value = e.message || '无法加载声音档案' }
  finally { loading.value = false }
})
function goBack() { uni.navigateBack() }
</script>

<style lang="scss" scoped>
.archive-page { min-height: 100vh; box-sizing: border-box; padding-bottom: 150rpx; background: #f2f1ec; }
.archive-nav { display: flex; justify-content: space-between; align-items: center; padding: 0 32rpx 18rpx; background: #f7f7f3; color: #1d1e1e; }
.archive-nav view { width: 60rpx; font-size: 55rpx; line-height: 1; }
.archive-nav text { font-size: 30rpx; font-weight: 700; }
.archive-nav__end { visibility: hidden; }
.archive-state { padding: 100rpx 32rpx; text-align: center; color: #70746f; font-size: 25rpx; }
.paper { margin: 30rpx 30rpx 0; padding: 20rpx 20rpx 0; border: 1rpx solid #c9cac4; background: repeating-linear-gradient(0deg, rgba(82,75,63,.012) 0, rgba(82,75,63,.012) 1rpx, transparent 1rpx, transparent 5rpx), #f4f1e9; box-shadow: 0 18rpx 40rpx rgba(42,40,35,.10); color: #242522; }
.paper__masthead { display: flex; justify-content: space-between; align-items: flex-end; gap: 10rpx; padding: 5rpx 9rpx 17rpx; border-bottom: 3rpx solid #222321; }
.paper__masthead > view:first-child { display: flex; flex-direction: column; min-width: 0; }
.paper__eyebrow, .paper__sub, .paper__number { color: #858882; font-size: 14rpx; font-family: monospace; font-weight: 600; letter-spacing: 2rpx; }
.paper__brand { display: block; margin: 6rpx 0; color: #1f211f; font-size: 42rpx; font-family: Georgia, serif; font-weight: 700; letter-spacing: -2rpx; line-height: 1; }
.paper__brand-dot { color: $sz-brand; }
.paper__number { display: flex; flex-shrink: 0; flex-direction: column; align-items: flex-end; line-height: 1.5; }
.paper__number-value { color: #222321; font-size: 22rpx; font-weight: 700; letter-spacing: 0; }
.paper__identity { display: flex; min-height: 226rpx; border-bottom: 2rpx solid #252622; }
.paper__portrait { position: relative; display: flex; flex: 0 0 33%; justify-content: center; align-items: center; overflow: hidden; border-right: 2rpx solid #252622; background: linear-gradient(145deg,#d5d9da,#5c6875); color: #fff; font-size: 77rpx; font-weight: 700; }
.paper__portrait-image { width: 100%; height: 100%; filter: grayscale(1); }
.paper__portrait-label { position: absolute; bottom: 0; left: 0; width: 100%; box-sizing: border-box; padding: 5rpx; background: rgba(244,241,233,.82); color: #7d827e; text-align: center; font: 14rpx monospace; letter-spacing: 2rpx; }
.paper__fields { flex: 1; min-width: 0; }
.paper__field { display: flex; min-height: 56rpx; border-bottom: 1rpx solid #c9cac4; }
.paper__field:last-child { border-bottom: 0; }
.paper__field text:first-child { display: flex; align-items: center; flex: 0 0 43%; box-sizing: border-box; padding-left: 10rpx; border-right: 1rpx solid #c9cac4; color: #87908e; font: 14rpx monospace; letter-spacing: 1rpx; }
.paper__field text:last-child { display: flex; align-items: center; min-width: 0; box-sizing: border-box; padding: 0 9rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #252622; font-size: 20rpx; font-weight: 600; }
.paper__section { border-bottom: 2rpx solid #272824; }
.paper__section-heading { display: flex; justify-content: space-between; padding: 10rpx 9rpx; border-bottom: 1rpx solid #c4c6c0; color: #88908b; font: 15rpx monospace; letter-spacing: 1rpx; }
.paper__line-copy { display: flex; flex-direction: column; padding: 12rpx 9rpx 0; color: #30322d; font-size: 21rpx; line-height: 2.1; background: repeating-linear-gradient(0deg, transparent 0, transparent 49rpx, #c9cac4 50rpx, transparent 51rpx); }
.paper__stat-row { display: flex; align-items: center; gap: 18rpx; min-height: 68rpx; padding: 0 12rpx; border-bottom: 1rpx solid #d5d7d0; }
.paper__stat-row:last-child { border-bottom: 0; }
.paper__stat-row text:first-child { color: #9a9d97; font: 16rpx monospace; }
.paper__stat-row text:nth-child(2) { flex: 1; font-size: 22rpx; font-weight: 600; }
.paper__stat-row text:last-child { font: 700 24rpx monospace; }
.paper__note { padding: 18rpx 10rpx 22rpx; color: #5c625c; font-size: 19rpx; line-height: 1.7; }
.paper__signature { display: flex; justify-content: space-between; align-items: flex-end; min-height: 148rpx; padding: 16rpx 9rpx; }
.paper__signature-label { display: block; color: #8a8f89; font: 15rpx monospace; letter-spacing: 1rpx; }
.paper__signature-name { display: block; margin-top: 18rpx; min-width: 210rpx; padding-bottom: 4rpx; border-bottom: 1rpx solid #242522; color: #30312e; font-size: 27rpx; font-family: Georgia, 'Songti SC', serif; font-style: italic; }
.paper__seal { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 94rpx; height: 94rpx; border: 3rpx solid rgba(29,78,216,.36); border-radius: 50%; color: rgba(29,78,216,.7); font: 13rpx monospace; line-height: 1.4; text-align: center; transform: rotate(-13deg); }
.paper__footer { display: flex; justify-content: space-between; padding: 12rpx 8rpx; border-top: 1rpx solid #c9cac4; color: #8a8f89; font: 14rpx monospace; }
</style>
