<template>
  <!-- 上传歌曲弹窗（v2 决议 D9）：半屏玻璃弹窗，曲库搜索 + 冷却倒计时 -->
  <view v-if="visible" class="popup-mask" @click="onMaskClick">
    <view class="popup-sheet sz-glass" @click.stop>
      <view class="popup-sheet__handle" />
      <view class="popup-sheet__header">
        <view>
          <text class="popup-sheet__title">Upload a Song</text>
          <text class="popup-sheet__subtitle">Up to 10 minutes per track</text>
        </view>
        <text class="popup-sheet__close" @click="$emit('close')">✕</text>
      </view>

      <!-- 冷却状态：按钮置灰 + 倒计时（决议 D4） -->
      <view v-if="cooldown > 0" class="cooldown-tip">
        Cooldown active · next upload in {{ formatCooldown(cooldown) }}
      </view>

      <!-- 搜索框：极简横线 -->
      <view class="search-line">
        <text class="search-line__icon">⌕</text>
        <input
          class="search-line__input"
          v-model="keyword"
          placeholder="Search tracks..."
          placeholder-class="placeholder"
          :disabled="cooldown > 0"
        />
      </view>

      <!-- 搜索结果列表 -->
      <scroll-view class="track-list" scroll-y>
        <view
          v-for="track in tracks"
          :key="track.id"
          class="track-row"
          @click="onPick(track)"
        >
          <view class="track-row__cover" :style="{ backgroundColor: track.coverColor }">
            <image v-if="track.coverUrl" class="track-row__cover-image" :src="track.coverUrl" mode="aspectFill" />
            <text v-else class="track-row__note">♪</text>
          </view>
          <view class="track-row__info">
            <text class="track-row__title">{{ track.title }}</text>
            <text class="track-row__artist">{{ track.artist }}</text>
            <text class="track-row__source">{{ sourceLabel(track) }} · {{ formatDuration(track.durationSec) }}</text>
          </view>
          <text class="track-row__action" :class="{ 'track-row__action--disabled': cooldown > 0 }">{{ cooldown > 0 ? formatCooldown(cooldown) : 'Add' }}</text>
        </view>
        <view v-if="!tracks.length" class="track-list__empty">No matching tracks</view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
/**
 * UploadSongPopup 上传歌曲弹窗（v2：2026-09-24 决议 D4/D9）
 * @prop {Boolean} visible 显示状态
 * @prop {Number} cooldown 剩余冷却秒数（>0 时禁止上传并显示倒计时）
 * @prop {Number} zoneId 所在域
 * @emit close / uploaded(item) / toast(message)
 * 交互：搜索 → 点选即上传（冷却校验与过滤由 mock.uploadSong 完成，与后端一致）
 */
import { ref, watch } from 'vue'
import { searchTracks, uploadSong } from '@/api/mock.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  cooldown: { type: Number, default: 0 },
  zoneId: { type: Number, required: true },
})
const emit = defineEmits(['close', 'uploaded', 'toast'])

const keyword = ref('')
const tracks = ref([])
const uploading = ref(false)
let searchVersion = 0
let searchTimer = null

// 打开弹窗时加载全量曲库；搜索词变化实时过滤
watch(() => props.visible, async (v) => {
  if (v) {
    keyword.value = ''
    await loadTracks('')
  }
})
watch(keyword, async (kw) => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadTracks(kw), 350)
})

async function loadTracks(kw) {
  const version = ++searchVersion
  try { const results = await searchTracks(kw); if (version === searchVersion) tracks.value = results }
  catch (e) { emit('toast', e.message) }
}
async function onPick(track) {
  if (props.cooldown > 0 || uploading.value) return // 冷却中禁止上传
  uploading.value = true
  try {
    // 真实接口：成功返回 QueueItemDTO，失败由 request 层 reject（带 message）
    const item = await uploadSong(props.zoneId, track.id)
    emit('uploaded', item)
    emit('toast', item.status === 'PRESET' ? 'Saved to the preloaded queue' : 'Added to queue')
    emit('close')
  } catch (e) {
    // 冷却 / 过滤拒绝：轻量提示（决议：不使用强弹窗打断）
    emit('toast', e.message || 'Upload failed')
  } finally { uploading.value = false }
}

function onMaskClick() {
  emit('close')
}

function formatCooldown(sec) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

function formatDuration(sec) {
  const value = Math.max(0, Number(sec) || 0)
  return `${Math.floor(value / 60)}:${String(value % 60).padStart(2, '0')}`
}

function sourceLabel(track) {
  if (track.attribution) return track.attribution
  if (track.source === 'AUDIUS') return 'Audius'
  if (track.source === 'LOCAL_LICENSED') return 'Licensed library'
  return 'Track metadata'
}
</script>

<style lang="scss" scoped>
.popup-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.30);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: flex-end;
  z-index: 100;
}

/* 半屏玻璃弹窗：边缘轻微高光 + 软阴影（视觉规范） */
.popup-sheet {
  width: 100%;
  height: 50vh;
  min-height: 600rpx;
  border-radius: 48rpx 48rpx 0 0;
  padding: 34rpx 40rpx calc(50rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  background: rgba(255,255,255,.72);

  &__handle {
    width: 80rpx;
    height: 8rpx;
    border-radius: 999rpx;
    background-color: rgba(0, 0, 0, 0.12);
    margin: 0 auto 28rpx;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 28rpx;
  }

  &__title {
    font-size: 32rpx;
    font-weight: 600;
    display: block;
  }

  &__subtitle {
    display: block;
    margin-top: 4rpx;
    color: $sz-text-tertiary;
    font-size: 20rpx;
  }

  &__close {
    color: $sz-text-tertiary;
    font-size: $sz-font-base;
    padding: 8rpx;
  }
}

.cooldown-tip {
  font-size: 21rpx;
  color: $sz-text-secondary;
  background-color: rgba(0,0,0,.055);
  border-radius: 18rpx;
  padding: 12rpx 20rpx;
  margin-bottom: $sz-gap-sm;
  text-align: center;
}

/* 极简横线搜索栏（决议 D9：不用厚重输入框） */
.search-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: rgba(0,0,0,.055);
  border-radius: 18rpx;
  padding: 14rpx 22rpx;
  margin-bottom: 20rpx;

  &__icon {
    color: $sz-text-tertiary;
    font-size: $sz-font-lg;
  }

  &__input {
    flex: 1;
    font-size: 25rpx;
  }
}

.placeholder {
  color: $sz-text-tertiary;
}

.track-list {
  flex: 1;
  min-height: 0;

  &__empty {
    text-align: center;
    color: $sz-text-tertiary;
    font-size: $sz-font-sm;
    padding: 60rpx 0;
  }
}

/* 列表简洁：无厚重分割线（决议 D9） */
.track-row {
  display: flex;
  align-items: center;
  gap: $sz-gap-sm;
  padding: 18rpx 4rpx;
  border-bottom: 1rpx solid rgba(0,0,0,.06);

  &__cover {
    width: 78rpx;
    height: 78rpx;
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    overflow: hidden;
  }

  &__cover-image {
    width: 100%;
    height: 100%;
  }

  &__note {
    color: #ffffff;
    font-size: 30rpx;
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  &__title {
    font-size: 25rpx;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__artist {
    font-size: 21rpx;
    color: $sz-text-secondary;
  }

  &__source {
    font-size: 18rpx;
    color: $sz-text-tertiary;
  }

  &__action {
    padding: 8rpx 22rpx;
    background: $sz-primary;
    color: #fff;
    border-radius: 999rpx;
    font-size: 21rpx;
    &--disabled { background: rgba(0,0,0,.09); color: $sz-text-tertiary; }
  }
}
</style>
