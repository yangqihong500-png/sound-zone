<template>
  <view class="page">
    <view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view><text class="nav__title">Profile</text><view class="nav__back" />
    </view>
    <view v-if="loading" class="state"><text>Loading…</text></view>
    <view v-else-if="error" class="state"><text class="state__icon">?</text><text>{{ error }}</text><button class="state__button" @click="goBack">返回</button></view>
    <view v-else-if="user" class="page__body">
      <profile-hero :user="user" :cover-url="coverUrl" :avatar-url="avatarUrl" />
      <text class="page__description">在 SoundZone 一起听歌的人，声音总会留下痕迹。</text>
      <view class="page__actions">
        <button v-if="isMe" class="page__button page__button--dark" @click="goMe">回到 Me</button>
        <button v-else class="page__button page__button--dark" :disabled="followBusy" @click="onFollow">{{ followed ? '已关注' : '关注' }}</button>
        <button v-if="!isMe && followed" class="page__button" @click="openChat">发消息</button>
        <button class="page__button" @click="openArchive">声音档案</button>
      </view>
      <view class="stats">
        <view class="stats__item"><text class="stats__value">{{ user.stats?.uploads || 0 }}</text><text class="stats__label">上传歌曲</text></view>
        <view class="stats__item"><text class="stats__value">{{ user.stats?.likes || 0 }}</text><text class="stats__label">收到喜欢</text></view>
        <view class="stats__item"><text class="stats__value">{{ user.stats?.moments || 0 }}</text><text class="stats__label">分享瞬间</text></view>
      </view>
      <view class="section-heading"><text>公开声音档案</text><text class="section-heading__aside">关于 {{ user.name }}</text></view>
      <view class="archive-entry" @click="openArchive">
        <view class="archive-entry__art"><text>♪</text></view>
        <view class="archive-entry__copy"><text class="archive-entry__title">打开纸本音乐档案</text><text class="archive-entry__caption">昵称、声音记录与公开统计</text></view>
        <text class="archive-entry__arrow">›</text>
      </view>
      <view class="page__privacy"><text>私密域、收藏和关注列表不会在这里展示。</text></view>
    </view>
    <suspended-zone-player />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getUserProfile, followUser, unfollowUser, resolveMediaUrl } from '@/api/mock.js'
import { session } from '@/api/session.js'
import ProfileHero from '@/components/profile-hero/profile-hero.vue'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const user = ref(null)
const loading = ref(true)
const error = ref('')
const followed = ref(false)
const followBusy = ref(false)
const isMe = computed(() => user.value && user.value.id === session.userId)
const coverUrl = computed(() => resolveMediaUrl(user.value?.coverUrl))
const avatarUrl = computed(() => resolveMediaUrl(user.value?.avatarUrl))

onLoad(async (option) => {
  const userId = Number(option.userId)
  if (!option.userId || !Number.isInteger(userId) || userId <= 0) { loading.value = false; error.value = '无效的用户 ID'; return }
  try { user.value = await getUserProfile(userId); followed.value = Boolean(user.value.followed) }
  catch (e) { error.value = e.message || '无法加载个人主页' }
  finally { loading.value = false }
})
async function onFollow() {
  if (followBusy.value || !user.value) return
  followBusy.value = true
  try {
    if (followed.value) { await unfollowUser(user.value.id); followed.value = false }
    else { await followUser(user.value.id); followed.value = true }
  } catch (e) { uni.showToast({ title: e.message || '操作失败', icon: 'none' }) }
  finally { followBusy.value = false }
}
function goBack() { uni.navigateBack() }
function goMe() { uni.switchTab({ url: '/pages/user/index' }) }
function openChat() { if (user.value) uni.navigateTo({ url: `/pages/message/chat?userId=${user.value.id}` }) }
function openArchive() { if (user.value) uni.navigateTo({ url: `/pages/user/archive?userId=${user.value.id}` }) }
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; box-sizing: border-box; background: $sz-bg; }
.nav { display: flex; align-items: center; justify-content: space-between; padding: 0 40rpx 20rpx; background: rgba(255,255,255,.82); }
.nav__back { width: 60rpx; color: $sz-text; font-size: 56rpx; line-height: 1; }
.nav__title { color: $sz-text; font-size: 32rpx; font-weight: 700; }
.page__body { padding: 28rpx 32rpx 170rpx; }
.page__description { display: block; margin: 30rpx 20rpx 0; color: $sz-text-secondary; font-size: 23rpx; line-height: 1.5; }
.page__actions { display: flex; flex-wrap: wrap; gap: 14rpx; margin: 27rpx 20rpx 0; }
.page__button { margin: 0; min-height: 70rpx; padding: 0 28rpx; border: 1rpx solid #d8dfe8; border-radius: 999rpx; background: #fff; color: $sz-text; font-size: 22rpx; font-weight: 650; }
.page__button--dark { border-color: $sz-control; background: $sz-control; color: #fff; }
.page__button[disabled] { opacity: .55; }
.stats { display: flex; margin: 38rpx 12rpx 0; padding: 28rpx 0; border-top: 1rpx solid #e2e7ee; border-bottom: 1rpx solid #e2e7ee; }
.stats__item { flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: center; }
.stats__item + .stats__item { border-left: 1rpx solid #e2e7ee; }
.stats__value { color: $sz-text; font-size: 38rpx; font-weight: 700; line-height: 1.15; }
.stats__label { margin-top: 7rpx; color: $sz-text-tertiary; font-size: 19rpx; }
.section-heading { display: flex; align-items: baseline; justify-content: space-between; margin: 44rpx 4rpx 19rpx; color: $sz-text; font-size: 30rpx; font-weight: 700; }
.section-heading__aside { color: $sz-text-tertiary; font-size: 19rpx; font-weight: 400; }
.archive-entry { display: flex; align-items: center; gap: 20rpx; min-height: 125rpx; box-sizing: border-box; padding: 20rpx 24rpx; border: 1rpx solid #e5e9ef; border-radius: 32rpx; background: #fff; }
.archive-entry__art { display: flex; flex-shrink: 0; align-items: center; justify-content: center; width: 76rpx; height: 76rpx; border-radius: 22rpx; background: linear-gradient(145deg, #8b8bad, #252948); color: #fff; font-size: 36rpx; }
.archive-entry__copy { display: flex; flex: 1; min-width: 0; flex-direction: column; }
.archive-entry__title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: $sz-text; font-size: 25rpx; font-weight: 700; }
.archive-entry__caption { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 5rpx; color: $sz-text-tertiary; font-size: 19rpx; }
.archive-entry__arrow { color: #929aa7; font-size: 34rpx; }
.page__privacy { margin: 26rpx 8rpx 0; color: $sz-text-tertiary; font-size: 19rpx; }
.state { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 20rpx; min-height: 55vh; color: $sz-text-secondary; font-size: 25rpx; }
.state__icon { color: $sz-text-tertiary; font-size: 80rpx; }
.state__button { padding: 8rpx 40rpx; border-radius: 999rpx; background: #fff; color: $sz-text; font-size: 23rpx; }
</style>
