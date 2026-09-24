<template>
  <!-- 上传歌曲弹窗（v2 决议 D9）：半屏玻璃弹窗，曲库搜索 + 冷却倒计时 -->
  <view v-if="visible" class="popup-mask" @click="onMaskClick">
    <view class="popup-sheet sz-glass" @click.stop>
      <view class="popup-sheet__handle" />
      <view class="popup-sheet__header">
        <text class="popup-sheet__title">上传歌曲</text>
        <text class="popup-sheet__close" @click="$emit('close')">✕</text>
      </view>

      <!-- 冷却状态：按钮置灰 + 倒计时（决议 D4） -->
      <view v-if="cooldown > 0" class="cooldown-tip">
        {{ formatCooldown(cooldown) }} 后可上传
      </view>

      <!-- 搜索框：极简横线 -->
      <view class="search-line">
        <text class="search-line__icon">⌕</text>
        <input
          class="search-line__input"
          v-model="keyword"
          placeholder="搜索曲库"
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
            <text class="track-row__note">♪</text>
          </view>
          <view class="track-row__info">
            <text class="track-row__title">{{ track.title }}</text>
            <text class="track-row__artist">{{ track.artist }}</text>
          </view>
          <text class="track-row__tags">{{ track.tags.slice(0, 2).join(' · ') }}</text>
        </view>
        <view v-if="!tracks.length" class="track-list__empty">没有匹配的曲目</view>
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

// 打开弹窗时加载全量曲库；搜索词变化实时过滤
watch(() => props.visible, async (v) => {
  if (v) {
    keyword.value = ''
    tracks.value = await searchTracks('')
  }
})
watch(keyword, async (kw) => {
  tracks.value = await searchTracks(kw)
})

async function onPick(track) {
  if (props.cooldown > 0) return // 冷却中禁止上传
  const res = await uploadSong(props.zoneId, track.id)
  if (res.code === 0) {
    emit('uploaded', res.queueItem)
    emit('toast', '已加入歌单')
    emit('close')
  } else {
    // 冷却 / 过滤拒绝：轻量提示（决议：不使用强弹窗打断）
    emit('toast', res.message)
  }
}

function onMaskClick() {
  emit('close')
}

function formatCooldown(sec) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}
</script>

<style lang="scss" scoped>
.popup-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: flex-end;
  z-index: 100;
}

/* 半屏玻璃弹窗：边缘轻微高光 + 软阴影（视觉规范） */
.popup-sheet {
  width: 100%;
  max-height: 70vh;
  border-radius: $sz-radius-lg $sz-radius-lg 0 0;
  padding: $sz-gap-md $sz-gap-md calc(#{$sz-gap-md} + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;

  &__handle {
    width: 72rpx;
    height: 8rpx;
    border-radius: 999rpx;
    background-color: rgba(0, 0, 0, 0.12);
    margin: 0 auto $sz-gap-sm;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $sz-gap-sm;
  }

  &__title {
    font-size: $sz-font-lg;
    font-weight: 500;
  }

  &__close {
    color: $sz-text-tertiary;
    font-size: $sz-font-base;
    padding: 8rpx;
  }
}

.cooldown-tip {
  font-size: $sz-font-sm;
  color: $sz-text-secondary;
  background-color: rgba(140, 155, 171, 0.15);
  border-radius: $sz-radius-sm;
  padding: 12rpx 20rpx;
  margin-bottom: $sz-gap-sm;
  text-align: center;
}

/* 极简横线搜索栏（决议 D9：不用厚重输入框） */
.search-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.15);
  padding-bottom: 12rpx;
  margin-bottom: $sz-gap-sm;

  &__icon {
    color: $sz-text-tertiary;
    font-size: $sz-font-lg;
  }

  &__input {
    flex: 1;
    font-size: $sz-font-base;
  }
}

.placeholder {
  color: $sz-text-tertiary;
}

.track-list {
  max-height: 44vh;

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
  padding: $sz-gap-sm 4rpx;

  &__cover {
    width: 72rpx;
    height: 72rpx;
    border-radius: $sz-radius-sm;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
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
    font-size: $sz-font-base;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__artist {
    font-size: $sz-font-xs;
    color: $sz-text-secondary;
  }

  &__tags {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }
}
</style>
