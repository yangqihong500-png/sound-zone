<template>
  <!-- 上传图片弹窗（v2 决议 D9）：半屏玻璃弹窗，选图绑定当前歌曲，跳过/上传 -->
  <view v-if="visible" class="popup-mask" @click="$emit('close')">
    <view class="popup-sheet sz-glass" @click.stop>
      <view class="popup-sheet__handle" />
      <view class="popup-sheet__header">
        <text class="popup-sheet__title">上传图片</text>
        <text class="popup-sheet__close" @click="$emit('close')">✕</text>
      </view>

      <!-- 图片选择区 -->
      <view class="picker" @click="chooseImage">
        <image v-if="imageUrl" class="picker__preview" :src="imageUrl" mode="aspectFill" />
        <view v-else class="picker__placeholder">
          <text class="picker__plus">＋</text>
          <text class="picker__hint">选择一张图片，分享当下日常</text>
        </view>
      </view>

      <!-- 绑定信息：图片与当前歌曲关联（决议 D6） -->
      <view v-if="bindTrack" class="bind-row">
        <text class="bind-row__label">将绑定歌曲</text>
        <text class="bind-row__track">♪ {{ bindTrack }}</text>
      </view>

      <!-- 双按钮：跳过图片 / 上传图片（决议 D9） -->
      <view class="actions">
        <button class="actions__btn actions__btn--ghost" @click="onSkip">跳过图片</button>
        <button
          class="actions__btn actions__btn--primary"
          :class="{ 'actions__btn--disabled': !imageUrl }"
          @click="onUpload"
        >上传图片</button>
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
})
const emit = defineEmits(['close', 'uploaded', 'toast'])

const imageUrl = ref('')

watch(() => props.visible, (v) => {
  if (v) imageUrl.value = ''
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
  if (!imageUrl.value) return
  const res = await uploadImage(props.zoneId, {
    imageUrl: imageUrl.value,
    trackTitle: props.bindTrack,
  })
  if (res.code === 0) {
    emit('uploaded', res.moment)
    emit('toast', '已分享到动态区')
    emit('close')
  } else {
    emit('toast', res.message)
  }
}

function onSkip() {
  // 「跳过图片」：不上传，仅关闭（图片为可选动作）
  emit('close')
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

.popup-sheet {
  width: 100%;
  border-radius: $sz-radius-lg $sz-radius-lg 0 0;
  padding: $sz-gap-md $sz-gap-md calc(#{$sz-gap-md} + env(safe-area-inset-bottom));

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
    margin-bottom: $sz-gap-md;
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

.picker {
  width: 100%;
  height: 360rpx;
  border-radius: $sz-radius-md;
  overflow: hidden;
  background-color: rgba(255, 255, 255, 0.5);

  &__preview {
    width: 100%;
    height: 100%;
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
    font-size: $sz-font-sm;
    color: $sz-text-tertiary;
  }
}

.bind-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $sz-gap-sm 4rpx;

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
  gap: $sz-gap-md;
  margin-top: $sz-gap-sm;

  &__btn {
    flex: 1;
    font-size: $sz-font-base;
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
