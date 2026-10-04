<template>
  <view class="profile-hero">
    <view class="profile-hero__art" :class="{ 'profile-hero__art--photo': coverUrl }">
      <image v-if="coverUrl" class="profile-hero__photo" :src="coverUrl" mode="aspectFill" aria-label="用户上传的公开主页背景" />
      <view v-if="coverUrl" class="profile-hero__scrim" />
      <text class="profile-hero__eyebrow">{{ owner ? 'MY SOUNDZONE' : 'SOUNDZONE' }} · LISTENER {{ serial }}</text>
      <text class="profile-hero__headline">{{ owner ? '你的声音，\n有自己的形状。' : '把夜晚\n留给音乐。' }}</text>
      <text class="profile-hero__subtitle">{{ owner ? 'THIS IS YOUR SPACE' : 'A LITTLE SPACE TO LISTEN' }}</text>
      <view class="profile-hero__record" aria-hidden="true"><view class="profile-hero__record-center" /></view>
      <view class="profile-hero__glass"><text>{{ owner ? 'MY SOUND FILE' : 'PUBLIC SOUND FILE' }}</text></view>
    </view>
    <view class="profile-hero__identity">
      <view class="profile-hero__avatar" :style="{ backgroundColor: user.avatarColor || '#8C9BAB' }">
        <image v-if="avatarUrl" class="profile-hero__avatar-image" :src="avatarUrl" mode="aspectFill" aria-label="用户头像" />
        <text v-else>{{ initial }}</text>
      </view>
      <view class="profile-hero__names">
        <text class="profile-hero__name">{{ user.name || 'Loading…' }}</text>
        <text class="profile-hero__handle">@{{ user.name || 'SoundZone' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  user: { type: Object, required: true },
  owner: { type: Boolean, default: false },
  coverUrl: { type: String, default: '' },
  avatarUrl: { type: String, default: '' },
})
const initial = computed(() => (props.user.name || '?').trim().charAt(0).toUpperCase() || '?')
const serial = computed(() => String(props.user.id || 0).padStart(3, '0'))
</script>

<style lang="scss" scoped>
.profile-hero { width: 100%; }
.profile-hero__art {
  position: relative; overflow: hidden; height: 356rpx; border-radius: 48rpx;
  background: linear-gradient(125deg, #d3dced 0%, #e8eaf3 49%, #d2d9e8 100%);
}
.profile-hero__photo, .profile-hero__scrim { position: absolute; inset: 0; width: 100%; height: 100%; }
.profile-hero__scrim { background: linear-gradient(90deg, rgba(8,17,30,.66) 0%, rgba(8,17,30,.38) 54%, rgba(8,17,30,.16) 100%); }
.profile-hero__art--photo .profile-hero__eyebrow,
.profile-hero__art--photo .profile-hero__headline,
.profile-hero__art--photo .profile-hero__subtitle { color: #fff; }
.profile-hero__art--photo .profile-hero__record { display: none; }
.profile-hero__eyebrow { position: relative; z-index: 2; display: block; padding: 36rpx 38rpx 0; color: #455a76; font-size: 19rpx; font-weight: 700; letter-spacing: 5rpx; }
.profile-hero__headline { position: relative; z-index: 2; display: block; padding: 48rpx 38rpx 0; max-width: 470rpx; white-space: pre-line; color: #1d3554; font-size: 53rpx; font-weight: 750; line-height: 1.13; letter-spacing: -2rpx; }
.profile-hero__subtitle { position: relative; z-index: 2; display: block; padding: 18rpx 38rpx 0; color: #677993; font-size: 19rpx; font-weight: 600; letter-spacing: 2rpx; }
.profile-hero__record { position: absolute; right: -85rpx; bottom: -210rpx; width: 430rpx; height: 430rpx; border-radius: 50%; background: #0d1d3a; box-shadow: inset 0 0 0 53rpx #182f57, inset 0 0 0 59rpx #213c65, 0 16rpx 32rpx rgba(18,35,62,.16); }
.profile-hero__record-center { position: absolute; top: 172rpx; left: 172rpx; width: 86rpx; height: 86rpx; border-radius: 50%; background: #42618b; box-shadow: inset 0 0 0 18rpx #29466e; }
.profile-hero__glass { position: absolute; z-index: 3; right: 22rpx; bottom: 21rpx; padding: 13rpx 20rpx; border: 1rpx solid rgba(255,255,255,.75); border-radius: 20rpx; background: rgba(255,255,255,.58); box-shadow: 0 10rpx 25rpx rgba(35,53,82,.12); backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px); }
.profile-hero__glass text { color: #294363; font-size: 18rpx; font-weight: 700; letter-spacing: 2rpx; }
.profile-hero__identity { display: flex; align-items: center; gap: 20rpx; min-width: 0; margin: 22rpx 24rpx 0; }
.profile-hero__avatar { display: flex; flex-shrink: 0; align-items: center; justify-content: center; overflow: hidden; width: 92rpx; height: 92rpx; border: 5rpx solid #fff; border-radius: 30rpx; box-shadow: 0 9rpx 24rpx rgba(35,57,84,.13); color: #fff; font-size: 43rpx; font-weight: 700; }
.profile-hero__avatar-image { width: 100%; height: 100%; }
.profile-hero__names { display: flex; flex: 1; min-width: 0; flex-direction: column; }
.profile-hero__name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: $sz-text; font-size: 32rpx; font-weight: 700; line-height: 1.2; }
.profile-hero__handle { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 4rpx; color: $sz-text-tertiary; font-size: 22rpx; }
</style>
