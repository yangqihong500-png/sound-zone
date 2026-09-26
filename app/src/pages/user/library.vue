<template>
  <view class="library">
    <text v-if="loading" class="empty">Loading…</text>
    <text v-else-if="error" class="empty">{{ error }}</text>
    <template v-else>
      <view v-for="entry in entries" :key="entry.id || entry.item?.itemId" class="sz-card row" @click="open(entry)">
        <template v-if="kind === 'zones'">
          <text>{{ entry.name }}</text><text class="secondary">{{ sceneName(entry.scene) }} · {{ entry.status === 'ENDED' ? 'Ended' : 'Live' }} · {{ entry.visibility === 'PRIVATE' ? 'Private' : 'Public' }}</text>
        </template>
        <template v-else-if="kind === 'uploads'">
          <text>{{ entry.item.title }} · {{ entry.item.artist }}</text><text class="secondary">{{ entry.zoneName }} · {{ statusNames[entry.item.status] }}</text>
        </template>
        <template v-else-if="kind === 'following'">
          <text>{{ entry.name }}</text><button class="small" @click.stop="unfollow(entry)">Unfollow</button>
        </template>
        <template v-else>
          <text>{{ entry.title }} · {{ entry.artist }}</text><button class="small" @click.stop="uncollect(entry)">Remove</button>
        </template>
      </view>
      <text v-if="!entries.length" class="empty">Nothing here yet.</text>
      <template v-if="kind === 'uploads' && images.length">
        <view class="section-label">My Moments</view>
        <view v-for="image in images" :key="image.id" class="image-entry">
          <text v-if="image.moderationStatus === 'REJECTED'" class="secondary">Unavailable</text>
          <moment-card :moment="image" :own="true" @withdraw="withdraw" />
        </view>
      </template>
    </template>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getMyList, unfollowUser, removeCollection, withdrawMoment } from '@/api/mock.js'
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
const withdraw = (id) => action(() => withdrawMoment(null, id))
</script>
<style lang="scss" scoped>
.library { min-height: 100vh; box-sizing: border-box; padding: 32rpx; background: $sz-bg; }
.row { margin-bottom: 18rpx; display: flex; flex-wrap: wrap; gap: 12rpx; align-items: center; justify-content: space-between; border-radius: 28rpx; font-size: 27rpx; font-weight: 500; }
.secondary { display: block; width: 100%; font-size: 21rpx; font-weight: 400; color: $sz-text-secondary; }
.empty { display: block; text-align: center; padding: 60rpx; color: $sz-text-tertiary; }
.small { font-size: 21rpx; background: rgba(0,0,0,.055); margin: 0; border-radius: 999rpx; color: $sz-text-secondary; }
.section-label { margin: 40rpx 0 22rpx; font-size: 27rpx; font-weight: 600; }
.image-entry { margin-bottom: $sz-gap-md; }
</style>
