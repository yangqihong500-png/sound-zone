<template>
  <view class="library">
    <text v-if="loading" class="empty">Loading…</text>
    <text v-else-if="error" class="empty">{{ error }}</text>
    <template v-else>
      <view v-if="kind === 'uploads' && entries.length" class="upload-ranking">
        <text class="upload-ranking__heart">♥</text>
        <text>Most liked first</text>
      </view>
      <view
        v-for="entry in entries"
        :key="entry.id || entry.item?.itemId"
        class="sz-card row"
        :class="{ 'row--upload': kind === 'uploads', 'row--favorite': kind === 'collections', 'row--following': kind === 'following' }"
        @click="open(entry)"
      >
        <template v-if="kind === 'zones'">
          <text>{{ entry.name }}</text><text class="secondary">{{ sceneName(entry.scene) }} · {{ entry.status === 'ENDED' ? 'Ended' : 'Live' }} · {{ entry.visibility === 'PRIVATE' ? 'Private' : 'Public' }}</text>
        </template>
        <template v-else-if="kind === 'uploads'">
          <view class="upload-track">
            <view class="upload-track__cover">
              <image v-if="entry.item.coverUrl" class="upload-track__cover-image" :src="entry.item.coverUrl" mode="aspectFill" />
              <text v-else class="upload-track__note">♪</text>
            </view>
            <view class="upload-track__copy">
              <text class="upload-track__title">{{ entry.item.title }}</text>
              <text class="upload-track__artist">{{ entry.item.artist }}</text>
              <text class="upload-track__meta">{{ entry.zoneName }} · {{ statusNames[entry.item.status] }}</text>
            </view>
            <view class="upload-track__likes" aria-label="Likes received">
              <text class="upload-track__heart">♥</text>
              <text class="upload-track__like-count">{{ entry.item.likes || 0 }}</text>
            </view>
          </view>
        </template>
        <template v-else-if="kind === 'following'">
          <view class="following-user">
            <view class="following-user__avatar" :style="{ backgroundColor: entry.avatarColor || '#A8B8C8' }">
              <text class="following-user__initial">{{ userInitial(entry.name) }}</text>
            </view>
            <text class="following-user__name">{{ entry.name }}</text>
          </view>
          <view class="row__actions">
            <button class="small small--message" @click.stop="openChat(entry.id)">Message</button>
            <button class="small" @click.stop="unfollow(entry)">Unfollow</button>
          </view>
        </template>
        <template v-else>
          <view class="favorite-track">
            <view class="favorite-track__cover" :style="{ backgroundColor: entry.coverColor || '#A8B8C8' }">
              <image v-if="entry.coverUrl" class="favorite-track__cover-image" :src="entry.coverUrl" mode="aspectFill" />
              <text v-else class="favorite-track__note">♪</text>
            </view>
            <view class="favorite-track__copy">
              <text class="favorite-track__title">{{ entry.title }}</text>
              <text class="favorite-track__artist">{{ entry.artist }}</text>
            </view>
          </view>
          <button class="small" @click.stop="uncollect(entry)">Remove</button>
        </template>
      </view>
      <text v-if="!entries.length" class="empty">Nothing here yet.</text>
      <template v-if="kind === 'uploads' && images.length">
        <view class="section-label">My Moments</view>
        <view v-for="image in images" :key="image.id" class="image-entry" @click="openMoment(image.id)">
          <text v-if="image.moderationStatus === 'REJECTED'" class="secondary">Unavailable</text>
          <moment-card :moment="image" @user="openMoment(image.id)" />
        </view>
      </template>
    </template>
    <suspended-zone-player />
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getMyList, unfollowUser, removeCollection } from '@/api/mock.js'
import MomentCard from '@/components/moment-card/moment-card.vue'
const kind = ref('zones')
const entries = ref([])
const images = ref([])
const loading = ref(true)
const error = ref('')
const names = { zones: 'My Zones', uploads: 'My Uploads', following: 'Following', collections: 'Favorites' }
const statusNames = { PLAYING: 'Playing', QUEUED: 'Queued', PRESET: 'Preloaded', PLAYED: 'Played', STOPPED: 'Zone ended', REMOVED: 'History' }
const scenes = { 音乐: 'Music', 自习: 'Study', 健身: 'Fitness', 旅行: 'Travel', 日系: 'J-Pop', 电子: 'Electronic', 工作: 'Work', 手工: 'Craft', 深夜: 'Late Night' }
function sceneName(scene) { return scenes[scene] || scene }
function userInitial(name) { return (name || '?').trim().charAt(0).toUpperCase() || '?' }
onLoad((option) => { kind.value = names[option.kind] ? option.kind : 'zones'; uni.setNavigationBarTitle({ title: names[kind.value] }) })
onShow(load)
async function load() {
  loading.value = true; error.value = ''
  try {
    entries.value = await getMyList(kind.value)
    if (kind.value === 'uploads') images.value = await getMyList('moments')
  } catch (e) { error.value = e.message }
  finally { loading.value = false }
}
function open(entry) {
  if (kind.value === 'following') uni.navigateTo({ url: `/pages/user/home?userId=${entry.id}` })
  if (kind.value === 'zones' && entry.status === 'ACTIVE') uni.navigateTo({ url: `/pages/zone/detail?id=${entry.id}` })
  if (kind.value === 'uploads' && entry.zoneStatus === 'ACTIVE') uni.navigateTo({ url: `/pages/zone/detail?id=${entry.zoneId}` })
}
async function action(work) {
  try { await work(); await load() } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
}
const unfollow = (entry) => action(() => unfollowUser(entry.id))
const uncollect = (entry) => action(() => removeCollection(entry.id))
const openMoment = (id) => uni.navigateTo({ url: `/pages/zone/moment-detail?id=${id}` })
const openChat = (userId) => uni.navigateTo({ url: `/pages/message/chat?userId=${userId}` })
</script>
<style lang="scss" scoped>
.library { min-height: 100vh; box-sizing: border-box; padding: 32rpx; background: $sz-bg; }
.row { margin-bottom: 18rpx; display: flex; flex-wrap: wrap; gap: 12rpx; align-items: center; justify-content: space-between; border-radius: 28rpx; font-size: 27rpx; font-weight: 500; }
.row--upload { flex-wrap: nowrap; padding: 22rpx; }
.row--favorite { flex-wrap: nowrap; gap: 20rpx; }
.row--favorite .small { flex-shrink: 0; }
.row--following { flex-wrap: nowrap; gap: 20rpx; }
.secondary { display: block; width: 100%; font-size: 21rpx; font-weight: 400; color: $sz-text-secondary; }
.empty { display: block; text-align: center; padding: 60rpx; color: $sz-text-tertiary; }
.small { margin: 0; border: 1rpx solid $sz-control-border; border-radius: 999rpx; background: $sz-control-soft; color: $sz-control; font-size: 21rpx; font-weight: 600; }
.row__actions { display: flex; flex-shrink: 0; align-items: center; gap: 12rpx; }
.small--message { border-color: rgba(255,255,255,.68); background: $sz-control; box-shadow: 0 8rpx 20rpx rgba(0,0,0,.14); color: #fff; }
.section-label { margin: 40rpx 0 22rpx; font-size: 27rpx; font-weight: 600; }
.image-entry { margin-bottom: $sz-gap-md; }

.upload-ranking {
  margin: 0 4rpx 18rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8rpx;
  color: $sz-text-tertiary;
  font-size: 20rpx;

  &__heart { color: $sz-control; font-size: 22rpx; }
}

.upload-track {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 18rpx;

  &__cover {
    width: 104rpx;
    height: 104rpx;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    border-radius: 20rpx;
    background: linear-gradient(145deg, #A8B8C8, #C3B8D9);
  }

  &__cover-image { width: 100%; height: 100%; }
  &__note { color: rgba(255,255,255,.9); font-size: 38rpx; }

  &__copy {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4rpx;
  }

  &__title,
  &__artist,
  &__meta {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__title { color: $sz-text; font-size: 27rpx; font-weight: 650; }
  &__artist { color: $sz-text-secondary; font-size: 22rpx; font-weight: 450; }
  &__meta { color: $sz-text-tertiary; font-size: 19rpx; font-weight: 400; }

  &__likes {
    min-width: 68rpx;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 7rpx;
    color: $sz-control;
    font-variant-numeric: tabular-nums;
  }

  &__heart { font-size: 27rpx; }
  &__like-count { font-size: 24rpx; font-weight: 650; }
}

.following-user {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: 18rpx;

  &__avatar {
    width: 82rpx;
    height: 82rpx;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 3rpx solid rgba(255,255,255,.88);
    border-radius: 50%;
    box-shadow: 0 8rpx 22rpx rgba(40,44,52,.13);
  }

  &__initial { color: #fff; font-size: 32rpx; font-weight: 600; }

  &__name {
    min-width: 0;
    overflow: hidden;
    color: $sz-text;
    font-size: 27rpx;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.favorite-track {
  display: flex;
  align-items: center;
  gap: 18rpx;
  flex: 1;
  min-width: 0;

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

  &__copy {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4rpx;
  }

  &__title,
  &__artist {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__title { font-size: 27rpx; font-weight: 600; color: $sz-text; }
  &__artist { font-size: 22rpx; font-weight: 400; color: $sz-text-secondary; }
}
</style>
