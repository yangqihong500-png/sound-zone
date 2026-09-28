<template>
  <view class="zone-card" :class="'zone-card--' + size" :style="cardStyle" @click="goDetail">
    <view class="zone-card__visual">
      <image v-if="zone.nowPlaying?.coverUrl" class="zone-card__cover-image" :src="zone.nowPlaying.coverUrl" mode="aspectFill" />
      <view v-else class="zone-card__cover-fallback">
        <text class="zone-card__theme-mark">{{ theme.icon }}</text>
        <view class="zone-card__texture" />
      </view>
      <view class="zone-card__scrim" />
      <view v-if="featured" class="zone-card__enter sz-glass-clear">Enter →</view>
      <view class="zone-card__meta">
        <view class="zone-card__chip sz-glass-clear"><text>{{ theme.icon }}</text><text>{{ sceneLabel }}</text></view>
        <view class="zone-card__live sz-glass-clear"><view class="zone-card__live-dot" /><text>LIVE</text></view>
        <text class="zone-card__listeners">{{ featured ? zone.listeners + ' with you' : zone.listeners }}</text>
      </view>
    </view>
    <view class="zone-card__info">
      <text class="zone-card__name">{{ zone.name }}</text>
      <text v-if="zone.searchMatch" class="zone-card__track zone-card__track--match">{{ searchMatchLabel }} · {{ zone.searchMatch.title }} · {{ zone.searchMatch.artist }}</text>
      <text v-else-if="zone.nowPlaying" class="zone-card__track">{{ zone.nowPlaying.title }} · {{ zone.nowPlaying.artist }}</text>
      <text v-else class="zone-card__track">Waiting for the first track</text>
      <view class="zone-card__details">
        <text v-if="zone.filterTags?.length" class="zone-card__filter">{{ filterSummary }}</text>
        <text v-if="zone.visibility === 'PRIVATE'" class="zone-card__private">PRIVATE</text>
        <text class="zone-card__host">@{{ zone.host }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  zone: { type: Object, required: true },
  size: { type: String, default: 'feed' },
  featured: { type: Boolean, default: false },
})

const themes = {
  自习: { label: 'Study', icon: '✎', color: '#A8B8C8' },
  健身: { label: 'Fitness', icon: '⌁', color: '#A9C4B5' },
  旅行: { label: 'Travel', icon: '✈', color: '#D9CFB8' },
  日系: { label: 'J-Pop', icon: '✿', color: '#E3C9CD' },
  深夜: { label: 'Late Night', icon: '☾', color: '#C3B8D9' },
  音乐: { label: 'Music', icon: '♫', color: '#3B6EA8' },
  电子: { label: 'Electronic', icon: '⌁', color: '#3B6EA8' },
  工作: { label: 'Work', icon: '⌘', color: '#A8B8C8' },
  手工: { label: 'Craft', icon: '◇', color: '#D9CFB8' },
}
const theme = computed(() => themes[props.zone.scene] || { label: props.zone.scene || 'Zone', icon: '◌', color: '#3B6EA8' })
const sceneLabel = computed(() => {
  if (props.featured) return theme.value.label
  return { 'Late Night': 'Night', Electronic: 'Electro' }[theme.value.label] || theme.value.label
})
const accent = computed(() => props.zone.coverColor || theme.value.color)
const searchMatchLabel = computed(() => {
  if (props.zone.searchMatch?.type === 'QUEUED') return `Up next #${props.zone.searchMatch.position}`
  if (props.zone.searchMatch?.type === 'PRESET') return `Saved for later #${props.zone.searchMatch.position}`
  return 'Playing now'
})
const filterSummary = computed(() => {
  const tags = props.zone.filterTags || []
  const visible = tags.slice(0, 2).join(' · ')
  const more = tags.length > 2 ? ` +${tags.length - 2}` : ''
  return `${props.zone.filterMode === 'BAN' ? 'Blocked' : 'Allowed'} · ${visible}${more}`
})
const cardStyle = computed(() => ({
  backgroundColor: accent.value,
  boxShadow: '0 18rpx 54rpx rgba(30,55,84,.14)',
}))

function goDetail() {
  uni.navigateTo({ url: '/pages/zone/detail?id=' + props.zone.id })
}
</script>

<style lang="scss" scoped>
.zone-card {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 390rpx;
  overflow: hidden;
  border-radius: 48rpx;
  background: rgba(255,255,255,.9);
  box-shadow: $sz-shadow-soft;
  &--tall { height: 470rpx; }
  &--short { height: 350rpx; }
  &--featured { height: 680rpx; }
  &__visual {
    position: relative;
    flex: 1;
    min-height: 0;
    overflow: hidden;
    background: inherit;
  }
  &__cover-image, &__cover-fallback, &__scrim, &__texture {
    position: absolute; inset: 0; width: 100%; height: 100%;
  }
  &__cover-fallback {
    display: flex; align-items: center; justify-content: center;
    background: linear-gradient(145deg, rgba(255,255,255,.26), rgba(0,0,0,.08));
  }
  &__theme-mark { color: rgba(255,255,255,.68); font-size: 112rpx; font-weight: 300; }
  &__texture {
    opacity: .18;
    background-image: radial-gradient(rgba(255,255,255,.75) 1rpx, transparent 1rpx);
    background-size: 22rpx 22rpx;
  }
  &__scrim { background: linear-gradient(to top, rgba(10,16,28,.34), transparent 40%, rgba(10,16,28,.05)); }
  &__enter {
    position: absolute; top: 28rpx; right: 28rpx; z-index: 2;
    padding: 12rpx 28rpx; border: 1rpx solid rgba(255,255,255,.72); border-radius: 999rpx;
    background: $sz-primary; color: #fff; font-size: 24rpx; font-weight: 600;
    box-shadow: 0 12rpx 28rpx rgba(15,23,42,.24), inset 0 1rpx 0 rgba(255,255,255,.2);
    backdrop-filter: none; -webkit-backdrop-filter: none;
  }
  &__meta {
    position: absolute;
    left: 20rpx;
    right: 20rpx;
    bottom: 18rpx;
    display: flex;
    align-items: center;
    gap: 10rpx;
  }
  &__chip, &__live {
    display: flex; align-items: center; gap: 7rpx; padding: 5rpx 13rpx;
    flex: 0 0 auto;
    border-radius: 999rpx; color: #fff; font-size: 20rpx; line-height: 1.35; white-space: nowrap;
  }
  &__chip text { white-space: nowrap; }
  &__live { font-size: 18rpx; font-weight: 600; letter-spacing: 1rpx; }
  &__live-dot { width: 9rpx; height: 9rpx; border-radius: 50%; background: #f08a88; box-shadow: 0 0 10rpx rgba(240,138,136,.75); }
  &__listeners { flex: 0 0 auto; min-width: 0; margin-left: auto; color: rgba(255,255,255,.78); font-size: 20rpx; text-align: right; white-space: nowrap; }
  &__info {
    flex: none;
    box-sizing: border-box;
    padding: 18rpx 20rpx 20rpx;
    background: rgba(255,255,255,.92);
    color: $sz-text;
    backdrop-filter: blur(24px) saturate(112%);
    -webkit-backdrop-filter: blur(24px) saturate(112%);
  }
  &__name, &__track, &__host, &__filter { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  &__name { font-size: 27rpx; font-weight: 650; line-height: 1.3; }
  &__track { margin-top: 4rpx; color: $sz-text-secondary; font-size: 20rpx; }
  &__track--match { color: $sz-primary; }
  &__details { display: flex; align-items: center; gap: 10rpx; margin-top: 10rpx; min-width: 0; }
  &__filter { flex: 1; min-width: 0; color: $sz-text-tertiary; font-size: 18rpx; }
  &__host { flex-shrink: 0; color: $sz-primary; font-size: 18rpx; }
  &__private { flex-shrink: 0; color: $sz-text-tertiary; font-size: 16rpx; letter-spacing: 1rpx; }
}
.zone-card--tall,
.zone-card--short {
  .zone-card__info { padding: 14rpx 16rpx 16rpx; }
  .zone-card__name { font-size: 23rpx; }
  .zone-card__track { font-size: 18rpx; }
  .zone-card__filter { display: none; }
  .zone-card__details { justify-content: flex-end; margin-top: 7rpx; }
  .zone-card__chip { padding-inline: 10rpx; }
  .zone-card__listeners { font-size: 18rpx; }
}
.zone-card--featured {
  .zone-card__info { padding: 22rpx 28rpx 24rpx; }
  .zone-card__name { font-size: 34rpx; }
  .zone-card__track { font-size: 23rpx; }
  .zone-card__host { font-size: 20rpx; }
  .zone-card__chip, .zone-card__listeners { font-size: 22rpx; }
}
</style>
