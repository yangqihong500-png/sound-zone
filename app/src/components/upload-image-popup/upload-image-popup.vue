<template>
  <!-- 上传图片弹窗（v2 决议 D9）：半屏玻璃弹窗，选图绑定本次上传歌曲，跳过/上传 -->
  <view v-if="visible" class="popup-mask" @click="$emit('close')">
    <view class="popup-sheet sz-glass" @click.stop>
      <view class="popup-sheet__handle" />
      <view class="popup-sheet__header">
        <text class="popup-sheet__title">Upload a Photo</text>
        <text class="popup-sheet__close" @click="$emit('close')">✕</text>
      </view>

      <text class="popup-sheet__subtitle">This will be bound to your uploaded track</text>
      <view class="picker" @click="chooseImage">
        <view v-if="imageUrl" class="picker__polaroid">
          <image class="picker__preview" :src="imageUrl" mode="aspectFill" />
          <text>{{ bindTrack }} · just now</text>
        </view>
        <view v-else class="picker__placeholder">
          <text class="picker__plus">＋</text>
          <text class="picker__hint">Choose a photo</text>
        </view>
      </view>

      <!-- 绑定信息：图片与本次上传歌曲关联（决议 D6） -->
      <view v-if="bindTrack" class="bind-row">
        <text class="bind-row__label">Bound to</text>
        <text class="bind-row__track">♪ {{ bindTrack }}</text>
      </view>

      <label class="consent"><checkbox :checked="trainingConsent" @click="trainingConsent = !trainingConsent" color="#8c9bab" />Use photo to improve recommendations (optional)</label>
      <text class="consent-note">Shared right away. You can withdraw it later.</text>
      <!-- 双按钮：跳过图片 / 上传图片（决议 D9） -->
      <view class="actions">
        <button class="actions__btn actions__btn--ghost" @click="onSkip">Skip</button>
        <button
          class="actions__btn actions__btn--primary"
          :class="{ 'actions__btn--disabled': !imageUrl || uploading }"
          :disabled="!imageUrl || uploading"
          @click="onUpload"
        >Share Moment</button>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * UploadImagePopup 上传图片弹窗（v2：2026-09-24 决议 D6/D9）
 * @prop {Boolean} visible 显示状态
 * @prop {String} bindTrack 绑定歌曲名（当前播放或刚上传的歌曲）
 * @prop {Number} zoneId 所在域
 * @emit close / uploaded(moment) / toast(message)
 * 反馈保持轻量：成功后关闭弹窗 + Toast，不用强弹窗打断
 */
import { ref, watch } from 'vue'
import { uploadImage } from '@/api/mock.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  bindTrack: { type: String, default: '' },
  zoneId: { type: Number, required: true },
  queueItemId: { type: Number, default: null },
})
const emit = defineEmits(['close', 'uploaded', 'toast'])

const imageUrl = ref('')
const trainingConsent = ref(false)
const uploading = ref(false)

watch(() => props.visible, (v) => {
  if (v) { imageUrl.value = ''; trainingConsent.value = false }
})

function chooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: (res) => {
      imageUrl.value = res.tempFilePaths[0]
    },
  })
}

async function onUpload() {
  if (!imageUrl.value || !props.queueItemId || uploading.value) return
  uploading.value = true
  try {
    const moment = await uploadImage(props.zoneId, {
      filePath: imageUrl.value,
      queueItemId: props.queueItemId,
      trainingConsent: trainingConsent.value,
    })
    emit('uploaded', moment)
    emit('toast', 'Shared to Moments')
    emit('close')
  } catch (e) {
    emit('toast', e.message || 'Upload failed')
  } finally { uploading.value = false }
}

function onSkip() {
  // 「跳过图片」：不上传，仅关闭（图片为可选动作）
  emit('close')
}
</script>

<style lang="scss" scoped>
.consent, .consent-note { display: block; font-size: 20rpx; color: $sz-text-secondary; padding: 8rpx 0; }
.popup-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.30);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: flex-end;
  z-index: 100;
}

.popup-sheet {
  width: 100%;
  border-radius: 48rpx 48rpx 0 0;
  padding: 34rpx 40rpx calc(50rpx + env(safe-area-inset-bottom));
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
    margin-bottom: 2rpx;
  }

  &__title {
    font-size: 32rpx;
    font-weight: 600;
  }

  &__close {
    color: $sz-text-tertiary;
    font-size: $sz-font-base;
    padding: 8rpx;
  }
  &__subtitle { color: $sz-text-tertiary; font-size: 22rpx; display: block; margin-bottom: 24rpx; }
}

.picker {
  width: 100%;
  height: 330rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0,0,0,.045);

  &__preview {
    width: 100%;
    height: 230rpx;
    border-radius: 10rpx;
  }
  &__polaroid {
    width: 260rpx;
    padding: 12rpx 12rpx 25rpx;
    border-radius: 16rpx;
    background: #fff;
    box-shadow: $sz-shadow-soft;
    transform: rotate(-1.5deg);
    text { display: block; text-align: center; color: $sz-text-tertiary; font-size: 17rpx; font-style: italic; margin-top: 10rpx; }
  }

  &__placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12rpx;
  }

  &__plus {
    font-size: 64rpx;
    color: $sz-text-tertiary;
    line-height: 1;
  }

  &__hint {
    font-size: 23rpx;
    color: $sz-text-tertiary;
  }
}

.bind-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18rpx 4rpx;

  &__label {
    font-size: $sz-font-sm;
    color: $sz-text-secondary;
  }

  &__track {
    font-size: $sz-font-sm;
    color: $sz-accent;
  }
}

.actions {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;

  &__btn {
    flex: 1;
    font-size: 25rpx;
    border-radius: 999rpx;

    &::after {
      border: none;
    }

    &--ghost {
      background-color: rgba(0, 0, 0, 0.06);
      color: $sz-text-secondary;
    }

    &--primary {
      background-color: $sz-primary;
      color: #ffffff;
      font-weight: 500;
    }

    &--disabled {
      opacity: 0.4;
    }
  }
}
</style>
