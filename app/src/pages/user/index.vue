<template>
  <view class="page">
    <profile-hero :user="user" owner :cover-url="coverUrl" :avatar-url="avatarUrl" />
    <view class="page__head-actions">
      <button class="page__action page__action--dark" :disabled="avatarBusy" @click="changeAvatar">{{ avatarBusy ? '上传中…' : '更换头像' }}</button>
      <button class="page__action" :disabled="nameBusy" @click="changeName">修改 ID</button>
      <button class="page__action" :disabled="coverBusy" @click="changeCover">{{ coverBusy ? '上传中…' : '更换背景' }}</button>
    </view>

    <view class="stats">
      <view class="stats__item"><text class="stats__value">{{ formatStat(user.stats?.uploads) }}</text><text class="stats__label">上传歌曲</text></view>
      <view class="stats__item"><text class="stats__value">{{ formatStat(user.stats?.moments) }}</text><text class="stats__label">分享瞬间</text></view>
      <view class="stats__item"><text class="stats__value">{{ zonesLoaded ? formatStat(zones.length) : '—' }}</text><text class="stats__label">创建的域</text></view>
    </view>

    <view class="section-heading"><text>我的聆听</text><text class="section-heading__aside">仅自己可见</text></view>
    <view class="listening-card" @click="openListening">
      <view class="listening-card__icon">♪</view>
      <view class="listening-card__copy"><text class="listening-card__title">Listening Time</text><text class="listening-card__caption">和音乐一起度过的时间</text></view>
      <text class="listening-card__value">{{ formatDuration(listening.totalSeconds) }}</text><text class="listening-card__arrow">›</text>
    </view>

    <view class="section-heading"><text>我的内容</text><text class="section-heading__aside">整理与管理</text></view>
    <view class="menu">
      <view v-for="item in menus" :key="item.kind" class="menu__item" @click="onMenu(item)">
        <text class="menu__icon">{{ item.icon }}</text><text class="menu__title">{{ item.title }}</text><text class="menu__meta">{{ menuCount(item) }}</text><text class="menu__arrow">›</text>
      </view>
    </view>

    <view class="section-heading"><text>公开主页</text><text class="section-heading__aside">别人眼中的你</text></view>
    <view class="archive-entry" @click="openArchive">
      <view class="archive-entry__art"><text>♪</text></view>
      <view class="archive-entry__copy"><text class="archive-entry__title">预览我的声音档案</text><text class="archive-entry__caption">纸本档案 · 仅展示公开资料</text></view>
      <text class="archive-entry__arrow">›</text>
    </view>

    <view v-if="session.passwordAuth" class="account-entry" @click="onAccount">
      <text>{{ session.guest ? '登录或注册账号' : '退出当前账号' }}</text><text>›</text>
    </view>
    <text v-else class="account-note">Guest demo · Account login requires HTTPS</text>
    <suspended-zone-player />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getCurrentUser, getListeningSummary, getMyList, resolveMediaUrl, updateProfileName, uploadProfileAvatar, uploadProfileCover } from '@/api/mock.js'
import { session, logoutAccount } from '@/api/session.js'
import ProfileHero from '@/components/profile-hero/profile-hero.vue'

const user = ref({ id: null, name: 'Loading…', avatarColor: '#8C9BAB', avatarUrl: '', coverUrl: '', stats: { uploads: 0, likes: 0, moments: 0, following: 0 } })
const listening = ref({ totalSeconds: 0 })
const zones = ref([])
const zonesLoaded = ref(false)
const coverBusy = ref(false)
const avatarBusy = ref(false)
const nameBusy = ref(false)
const coverUrl = computed(() => resolveMediaUrl(user.value.coverUrl))
const avatarUrl = computed(() => resolveMediaUrl(user.value.avatarUrl))
const menus = [
  { title: '我创建的域', kind: 'zones', icon: '◎' },
  { title: '上传的歌曲', kind: 'uploads', icon: '♫' },
  { title: '正在关注', kind: 'following', icon: '◇' },
  { title: '收藏的歌曲', kind: 'collections', icon: '♡' },
]

onShow(load)
async function load() {
  try { user.value = await getCurrentUser() }
  catch (e) { user.value.name = 'Unavailable'; uni.showToast({ title: e.message, icon: 'none' }) }
  try { listening.value = await getListeningSummary() }
  catch { listening.value = { totalSeconds: 0 } }
  try { zones.value = await getMyList('zones'); zonesLoaded.value = true }
  catch { zonesLoaded.value = false }
}
function formatStat(value) { return Number.isFinite(Number(value)) ? String(value) : '—' }
function menuCount(item) {
  if (item.kind === 'zones') return zonesLoaded.value ? `${zones.value.length} 个` : ''
  if (item.kind === 'uploads') return `${formatStat(user.value.stats?.uploads)} 首`
  if (item.kind === 'following') return `${formatStat(user.value.stats?.following)} 人`
  return ''
}
function onMenu(item) { uni.navigateTo({ url: '/pages/user/library?kind=' + item.kind }) }
function openListening() { uni.navigateTo({ url: '/pages/user/listening' }) }
function openArchive() { if (user.value.id) uni.navigateTo({ url: `/pages/user/archive?userId=${user.value.id}` }) }
function formatDuration(seconds = 0) {
  const minutes = Math.floor(Math.max(0, seconds) / 60)
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours}h ${rest}m` : `${hours}h`
}
function changeCover() {
  if (coverBusy.value) return
  uni.showModal({
    title: '公开主页背景',
    content: '你选择的照片会显示在公开主页，其他人也能看到。确认后继续选择照片。',
    confirmText: '选择照片',
    success: ({ confirm }) => {
      if (!confirm) return
      uni.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'], success: async ({ tempFilePaths }) => {
        const filePath = tempFilePaths?.[0]
        if (!filePath) return
        coverBusy.value = true
        try {
          const updated = await uploadProfileCover(filePath)
          user.value = { ...user.value, coverUrl: updated.coverUrl }
          uni.showToast({ title: '背景已更新', icon: 'none' })
        } catch (e) { uni.showToast({ title: e.message || '上传失败', icon: 'none' }) }
        finally { coverBusy.value = false }
      } })
    },
  })
}
function changeAvatar() {
  if (avatarBusy.value) return
  uni.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'], success: async ({ tempFilePaths }) => {
    const filePath = tempFilePaths?.[0]
    if (!filePath) return
    avatarBusy.value = true
    try {
      const updated = await uploadProfileAvatar(filePath)
      user.value = { ...user.value, avatarUrl: updated.avatarUrl }
      uni.showToast({ title: '头像已更新', icon: 'none' })
    } catch (e) { uni.showToast({ title: e.message || '上传失败', icon: 'none' }) }
    finally { avatarBusy.value = false }
  } })
}
function changeName() {
  if (nameBusy.value) return
  uni.showModal({
    title: '修改用户 ID',
    content: user.value.name || '',
    editable: true,
    placeholderText: '请输入新的用户 ID',
    confirmText: '保存',
    success: async ({ confirm, content }) => {
      if (!confirm) return
      const name = (content || '').trim()
      if (name.length < 2 || name.length > 32) { uni.showToast({ title: '请输入 2 到 32 个字符', icon: 'none' }); return }
      nameBusy.value = true
      try {
        const updated = await updateProfileName(name)
        user.value = { ...user.value, name: updated.name }
        session.name = updated.name
        uni.showToast({ title: '用户 ID 已更新', icon: 'none' })
      } catch (e) { uni.showToast({ title: e.message || '修改失败', icon: 'none' }) }
      finally { nameBusy.value = false }
    },
  })
}
async function onAccount() {
  if (session.guest) { uni.navigateTo({ url: '/pages/auth/account' }); return }
  uni.showModal({ title: 'Log out?', content: 'You can sign in again with your username and password.', success: async ({ confirm }) => {
    if (!confirm) return
    await logoutAccount()
    uni.navigateTo({ url: '/pages/auth/account' })
  } })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 30rpx 32rpx 170rpx; background: $sz-bg; }
.page__head-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 12rpx; margin: 22rpx 20rpx 0; }
.page__action { width: auto; min-width: 150rpx; margin: 0; padding: 11rpx 20rpx; border: 1rpx solid $sz-control-border; border-radius: 999rpx; background: #fff; color: $sz-control; font-size: 21rpx; font-weight: 600; white-space: nowrap; }
.page__action--dark { border-color: $sz-control; background: $sz-control; color: #fff; }
.page__action[disabled] { opacity: .55; }
.stats { display: flex; margin-top: 34rpx; padding: 27rpx 0; border-top: 1rpx solid #e2e7ee; border-bottom: 1rpx solid #e2e7ee; }
.stats__item { flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: center; }
.stats__item + .stats__item { border-left: 1rpx solid #e2e7ee; }
.stats__value { color: $sz-text; font-size: 38rpx; font-weight: 700; line-height: 1.15; }
.stats__label { margin-top: 7rpx; color: $sz-text-tertiary; font-size: 19rpx; }
.section-heading { display: flex; align-items: baseline; justify-content: space-between; margin: 42rpx 4rpx 17rpx; color: $sz-text; font-size: 30rpx; font-weight: 700; }
.section-heading__aside { color: $sz-text-tertiary; font-size: 19rpx; font-weight: 400; }
.listening-card, .archive-entry { display: flex; align-items: center; gap: 20rpx; min-height: 113rpx; box-sizing: border-box; padding: 20rpx 24rpx; border: 1rpx solid #e5e9ef; border-radius: 32rpx; background: #fff; }
.listening-card__icon { display: flex; flex-shrink: 0; align-items: center; justify-content: center; width: 66rpx; height: 66rpx; border-radius: 20rpx; background: #20242c; color: #fff; font-size: 34rpx; }
.listening-card__copy, .archive-entry__copy { display: flex; flex: 1; min-width: 0; flex-direction: column; }
.listening-card__title, .archive-entry__title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: $sz-text; font-size: 25rpx; font-weight: 700; }
.listening-card__caption, .archive-entry__caption { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 4rpx; color: $sz-text-tertiary; font-size: 19rpx; }
.listening-card__value { flex-shrink: 0; color: $sz-text; font-size: 32rpx; font-weight: 700; }
.listening-card__arrow, .archive-entry__arrow { flex-shrink: 0; color: #929aa7; font-size: 34rpx; }
.menu { overflow: hidden; border: 1rpx solid #e5e9ef; border-radius: 32rpx; background: #fff; }
.menu__item { display: flex; align-items: center; gap: 20rpx; min-height: 94rpx; padding: 0 24rpx; }
.menu__item + .menu__item { border-top: 1rpx solid #edf0f4; }
.menu__icon { display: flex; flex-shrink: 0; align-items: center; justify-content: center; width: 57rpx; height: 57rpx; border-radius: 18rpx; background: #f0f3f8; color: #363e4b; font-size: 30rpx; }
.menu__title { flex: 1; min-width: 0; color: $sz-text; font-size: 24rpx; font-weight: 600; }
.menu__meta { flex-shrink: 0; color: $sz-text-tertiary; font-size: 19rpx; }
.menu__arrow { flex-shrink: 0; color: #929aa7; font-size: 32rpx; }
.archive-entry__art { display: flex; flex-shrink: 0; align-items: center; justify-content: center; width: 76rpx; height: 76rpx; border-radius: 22rpx; background: linear-gradient(145deg, #8b8bad, #252948); color: white; font-size: 36rpx; }
.account-entry { display: flex; justify-content: space-between; margin: 42rpx 4rpx 0; padding: 20rpx 0; border-top: 1rpx solid #e2e7ee; color: $sz-text-secondary; font-size: 23rpx; }
.account-note { display: block; margin: 35rpx 4rpx 0; color: $sz-text-tertiary; font-size: 19rpx; }
</style>
