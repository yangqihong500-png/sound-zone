<template>
  <view class="notifications">
    <view class="notifications__nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="notifications__back" @click="goBack">‹</view>
      <text class="notifications__title">Notifications</text>
      <text class="notifications__read-all" :class="{ 'notifications__read-all--disabled': !unreadCount }" @click="readAll">全部已读</text>
    </view>

    <view class="notifications__intro">
      <text class="notifications__eyebrow">STAY IN SYNC</text>
      <text class="notifications__subtitle">共听发生的新消息，都收在这里。</text>
    </view>

    <scroll-view class="notifications__filters" scroll-x enhanced :show-scrollbar="false">
      <view class="notifications__filter-row">
        <view
          v-for="item in filters"
          :key="item.key"
          class="notifications__filter"
          :class="{ 'notifications__filter--active': filter === item.key }"
          @click="filter = item.key"
        >{{ item.label }}</view>
      </view>
    </scroll-view>

    <view v-if="loading" class="notifications__empty">正在加载…</view>
    <view v-else-if="error" class="notifications__empty">{{ error }}</view>
    <view v-else-if="!visibleNotifications.length" class="notifications__empty-card">
      <view class="notifications__empty-bell">♩</view>
      <text class="notifications__empty-title">暂时没有新动态</text>
      <text class="notifications__empty-copy">私信、域邀请、歌曲开播和点赞会出现在这里。</text>
    </view>
    <view v-else class="notifications__list">
      <view
        v-for="item in visibleNotifications"
        :key="item.id"
        class="notification-card"
        :class="{ 'notification-card--unread': !item.read }"
        @click="openNotification(item)"
      >
        <view class="notification-card__visual" :style="{ backgroundColor: item.actorAvatarColor || visualColor(item.type) }">
          <image v-if="item.coverUrl" class="notification-card__cover" :src="item.coverUrl" mode="aspectFill" />
          <text v-else-if="item.type === 'DIRECT_MESSAGE'" class="notification-card__initial">{{ initial(item.actorName) }}</text>
          <text v-else class="notification-card__symbol">{{ visualSymbol(item.type) }}</text>
        </view>
        <view class="notification-card__content">
          <view class="notification-card__headline">
            <text class="notification-card__title">{{ item.title }}</text>
            <view v-if="!item.read" class="notification-card__dot" />
          </view>
          <text class="notification-card__body">{{ item.body }}</text>
          <view class="notification-card__meta">
            <text>{{ relativeTime(item.updatedAt) }}</text>
            <text v-if="item.eventCount > 1" class="notification-card__count">{{ item.eventCount }} 条新动态</text>
          </view>
        </view>
        <text class="notification-card__chevron">›</text>
      </view>
    </view>
    <suspended-zone-player />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getNotifications, markAllNotificationsRead, markNotificationRead } from '@/api/mock.js'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const notifications = ref([])
const filter = ref('ALL')
const loading = ref(true)
const error = ref('')
const filters = [
  { key: 'ALL', label: 'All' },
  { key: 'MESSAGES', label: 'Messages' },
  { key: 'ZONES', label: 'Zones' },
  { key: 'LIKES', label: 'Likes' },
]
const unreadCount = computed(() => notifications.value.filter((item) => !item.read).length)
const visibleNotifications = computed(() => notifications.value.filter((item) => {
  if (filter.value === 'ALL') return true
  if (filter.value === 'MESSAGES') return item.type === 'DIRECT_MESSAGE'
  if (filter.value === 'ZONES') return ['ZONE_INVITE', 'TRACK_STARTED'].includes(item.type)
  return item.type === 'TRACK_LIKE'
}))

onShow(load)

async function load() {
  loading.value = true
  error.value = ''
  try { notifications.value = await getNotifications() }
  catch (e) { error.value = e.message }
  finally { loading.value = false }
}

async function readAll() {
  if (!unreadCount.value) return
  try {
    await markAllNotificationsRead()
    notifications.value = notifications.value.map((item) => ({ ...item, read: true }))
  } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
}

async function openNotification(item) {
  try {
    if (!item.read) {
      await markNotificationRead(item.id)
      item.read = true
    }
    if (item.type === 'DIRECT_MESSAGE' && item.actorId) {
      uni.navigateTo({ url: `/pages/message/chat?userId=${item.actorId}` })
      return
    }
    if (item.type === 'TRACK_LIKE') {
      uni.navigateTo({ url: '/pages/user/library?kind=uploads' })
      return
    }
    if (item.zoneId) {
      const invite = item.inviteCode ? `&inviteCode=${encodeURIComponent(item.inviteCode)}` : ''
      uni.navigateTo({ url: `/pages/zone/detail?id=${item.zoneId}${invite}` })
    }
  } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
}

function goBack() { uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) }) }
function initial(name) { return (name || '?').trim().charAt(0).toUpperCase() || '?' }
function visualSymbol(type) { return type === 'ZONE_INVITE' ? '↗' : type === 'TRACK_STARTED' ? '▶' : '♥' }
function visualColor(type) {
  return { ZONE_INVITE: '#B8D9F5', TRACK_STARTED: '#C7E5D8', TRACK_LIKE: '#F1C7D0' }[type] || '#D7DDEA'
}
function relativeTime(value) {
  const time = new Date(value).getTime()
  const seconds = Math.max(0, Math.floor((Date.now() - time) / 1000))
  if (seconds < 60) return '刚刚'
  if (seconds < 3600) return `${Math.floor(seconds / 60)} 分钟前`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} 小时前`
  if (seconds < 604800) return `${Math.floor(seconds / 86400)} 天前`
  return new Date(value).toLocaleDateString()
}
</script>

<style lang="scss" scoped>
.notifications { min-height: 100vh; padding-bottom: 150rpx; box-sizing: border-box; background: $sz-bg; }
.notifications__nav {
  min-height: 92rpx;
  padding-left: 30rpx;
  padding-right: 30rpx;
  display: grid;
  grid-template-columns: 150rpx 1fr 150rpx;
  align-items: center;
  border-bottom: 1rpx solid rgba(28,28,30,.05);
  background: rgba(247,249,252,.92);
}
.notifications__back { color: $sz-text; font-size: 64rpx; font-weight: 300; line-height: 1; }
.notifications__title { text-align: center; color: $sz-text; font-size: 32rpx; font-weight: 750; }
.notifications__read-all { text-align: right; color: $sz-brand; font-size: 22rpx; font-weight: 600; }
.notifications__read-all--disabled { color: $sz-text-tertiary; }
.notifications__intro { padding: 42rpx 40rpx 26rpx; display: flex; flex-direction: column; gap: 8rpx; }
.notifications__eyebrow { color: $sz-brand; font-size: 18rpx; font-weight: 750; letter-spacing: 2.4rpx; }
.notifications__subtitle { color: $sz-text-secondary; font-size: 24rpx; }
.notifications__filters { width: 100%; white-space: nowrap; margin-bottom: 26rpx; }
.notifications__filter-row { display: inline-flex; gap: 12rpx; padding: 0 40rpx; }
.notifications__filter {
  padding: 12rpx 26rpx;
  border: 1rpx solid $sz-control-border;
  border-radius: 999rpx;
  background: rgba(255,255,255,.8);
  color: $sz-text-secondary;
  font-size: 22rpx;
  font-weight: 550;
}
.notifications__filter--active { border-color: $sz-control; background: $sz-control; color: #fff; }
.notifications__list { padding: 0 30rpx; }
.notification-card {
  position: relative;
  margin-bottom: 18rpx;
  padding: 24rpx 22rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border: 1rpx solid rgba(28,28,30,.05);
  border-radius: 30rpx;
  background: rgba(255,255,255,.88);
  box-shadow: 0 14rpx 36rpx rgba(30,55,84,.06);
}
.notification-card--unread { border-color: rgba(29,78,216,.16); background: rgba(255,255,255,.98); }
.notification-card__visual {
  width: 92rpx;
  height: 92rpx;
  flex: 0 0 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 26rpx;
  color: #fff;
}
.notification-card__cover { width: 100%; height: 100%; }
.notification-card__initial { font-size: 35rpx; font-weight: 750; }
.notification-card__symbol { font-size: 31rpx; font-weight: 750; }
.notification-card__content { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 7rpx; }
.notification-card__headline { display: flex; align-items: center; gap: 10rpx; }
.notification-card__title { overflow: hidden; color: $sz-text; font-size: 25rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.notification-card__dot { width: 12rpx; height: 12rpx; flex: 0 0 12rpx; border-radius: 50%; background: $sz-brand; }
.notification-card__body { overflow: hidden; color: $sz-text-secondary; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
.notification-card__meta { display: flex; align-items: center; gap: 12rpx; color: $sz-text-tertiary; font-size: 19rpx; }
.notification-card__count { color: $sz-brand; font-weight: 600; }
.notification-card__chevron { color: $sz-text-tertiary; font-size: 38rpx; font-weight: 300; }
.notifications__empty { display: block; padding: 90rpx 30rpx; text-align: center; color: $sz-text-tertiary; font-size: 24rpx; }
.notifications__empty-card {
  margin: 44rpx 40rpx;
  padding: 68rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 13rpx;
  border: 1rpx solid rgba(28,28,30,.05);
  border-radius: 34rpx;
  background: rgba(255,255,255,.76);
}
.notifications__empty-bell { width: 88rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; border-radius: 28rpx; background: $sz-control-soft; color: $sz-brand; font-size: 38rpx; }
.notifications__empty-title { color: $sz-text; font-size: 27rpx; font-weight: 700; }
.notifications__empty-copy { max-width: 470rpx; text-align: center; color: $sz-text-tertiary; font-size: 21rpx; line-height: 1.55; }
</style>
