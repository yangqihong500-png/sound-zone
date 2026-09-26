<template>
  <view class="zone-card" :class="'zone-card--' + size" :style="cardStyle" @click="goDetail">
    <image v-if="zone.nowPlaying?.coverUrl" class="zone-card__cover-image" :src="zone.nowPlaying.coverUrl" mode="aspectFill" />
    <view v-else class="zone-card__cover-fallback">
      <text class="zone-card__theme-mark">{{ theme.icon }}</text>
      <view class="zone-card__texture" />
    </view>
    <view class="zone-card__scrim" />
    <view v-if="featured" class="zone-card__enter sz-glass-clear">Enter →</view>
    <view class="zone-card__info sz-glass-tinted" :style="{ backgroundColor: accent + '38' }">
      <view class="zone-card__meta">
        <view class="zone-card__chip sz-glass-clear"><text>{{ theme.icon }}</text><text>{{ theme.label }}</text></view>
        <view class="zone-card__live sz-glass-clear"><view class="zone-card__live-dot" /><text>LIVE</text></view>
        <text class="zone-card__listeners">{{ featured ? zone.listeners + ' with you' : zone.listeners }}</text>
      </view>
      <text class="zone-card__name">{{ zone.name }}</text>
      <text v-if="zone.nowPlaying" class="zone-card__track">{{ zone.nowPlaying.title }} · {{ zone.nowPlaying.artist }}</text>
      <text v-else class="zone-card__track">Waiting for the first track</text>
      <text v-if="zone.filterTags?.length" class="zone-card__filter">{{ zone.filterMode === 'BAN' ? 'Blocked' : 'Allowed' }}: {{ zone.filterTags.join(' · ') }}</text>
      <text class="zone-card__host">hosted by @{{ zone.host }}</text>
      <text v-if="zone.visibility === 'PRIVATE'" class="zone-card__private">PRIVATE</text>
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
  音乐: { label: 'Music', icon: '♫', color: '#8C9BAB' },
  电子: { label: 'Electronic', icon: '⌁', color: '#8C9BAB' },
  工作: { label: 'Work', icon: '⌘', color: '#A8B8C8' },
  手工: { label: 'Craft', icon: '◇', color: '#D9CFB8' },
}
const theme = computed(() => themes[props.zone.scene] || { label: props.zone.scene || 'Zone', icon: '◌', color: '#8C9BAB' })
const accent = computed(() => props.zone.coverColor || theme.value.color)
const cardStyle = computed(() => ({
  backgroundColor: accent.value,
  boxShadow: '0 16rpx 64rpx rgba(0,0,0,.12), 0 0 100rpx ' + accent.value + '30',
}))

function goDetail() {
  uni.navigateTo({ url: '/pages/zone/detail?id=' + props.zone.id })
}
</script>

<style lang="scss" scoped>
.zone-card {
  position: relative;
  width: 100%;
  height: 390rpx;
  overflow: hidden;
  border-radius: 48rpx;
  box-shadow: $sz-shadow-soft;
  &--tall { height: 470rpx; }
  &--short { height: 350rpx; }
  &--featured { height: 720rpx; }
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
  &__scrim { background: linear-gradient(to top, rgba(0,0,0,.46), rgba(0,0,0,.03) 68%, rgba(0,0,0,.08)); }
  &__enter {
    position: absolute; top: 28rpx; right: 28rpx; z-index: 2;
    padding: 12rpx 28rpx; border-radius: 999rpx; color: #fff; font-size: 24rpx; font-weight: 500;
  }
  &__info { position: absolute; left: 0; right: 0; bottom: 0; padding: 24rpx; color: #fff; }
  &__meta { display: flex; align-items: center; gap: 10rpx; margin-bottom: 12rpx; }
  &__chip, &__live {
    display: flex; align-items: center; gap: 7rpx; padding: 5rpx 13rpx;
    border-radius: 999rpx; color: #fff; font-size: 20rpx; line-height: 1.35; white-space: nowrap;
  }
  &__live { font-size: 18rpx; font-weight: 600; letter-spacing: 1rpx; }
  &__live-dot { width: 9rpx; height: 9rpx; border-radius: 50%; background: #f08a88; box-shadow: 0 0 10rpx rgba(240,138,136,.75); }
  &__listeners { flex: 1; min-width: 0; color: rgba(255,255,255,.78); font-size: 20rpx; text-align: right; white-space: nowrap; }
  &__name, &__track, &__host, &__filter { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  &__name { font-size: 30rpx; font-weight: 600; line-height: 1.35; }
  &__track { margin-top: 4rpx; color: rgba(255,255,255,.72); font-size: 22rpx; }
  &__filter { margin-top: 4rpx; color: rgba(255,255,255,.72); font-size: 19rpx; }
  &__host { margin-top: 3rpx; color: rgba(255,255,255,.52); font-size: 19rpx; }
  &__private { position: absolute; right: 22rpx; bottom: 18rpx; color: rgba(255,255,255,.6); font-size: 17rpx; letter-spacing: 1rpx; }
}
.zone-card--featured {
  .zone-card__info { padding: 30rpx; }
  .zone-card__name { font-size: 36rpx; }
  .zone-card__track { font-size: 25rpx; }
  .zone-card__host { font-size: 21rpx; }
  .zone-card__chip, .zone-card__listeners { font-size: 22rpx; }
}
</style>
