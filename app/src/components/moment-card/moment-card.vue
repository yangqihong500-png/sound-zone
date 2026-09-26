<template>
  <!-- 图片分享卡片 v2：图片与歌曲关联 + 上传者 + 可撤回（本人） -->
  <view class="moment-card sz-card" :class="{ 'moment-card--compact': compact }">
    <image
      v-if="imagePath"
      class="moment-card__image"
      :src="imagePath"
      mode="aspectFill"
    />
    <view v-else class="moment-card__image" :style="{ backgroundColor: moment.color }" />
    <view class="moment-card__body">
      <text v-if="moment.text && !compact" class="moment-card__text">{{ moment.text }}</text>
      <view class="moment-card__user" @click.stop="$emit('user', moment.userId)">
        <text class="moment-card__name">@{{ moment.by || moment.user }}</text>
        <text v-if="own" class="moment-card__withdraw" @click.stop="$emit('withdraw', moment.id)">Withdraw</text>
      </view>
      <view class="moment-card__meta">
        <text class="moment-card__time">{{ moment.time }}</text>
        <text class="moment-card__track">{{ moment.track }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { loadImage } from '@/api/mock.js'
/**
 * MomentCard 图片分享卡片（v2：2026-09-24 决议 D6）
 * @prop {Object} moment 后端 MomentDTO：{ id, userId, text, imageUrl, color, track, by, time }
 *   - userId: 上传者 ID（点击跳转用户主页用）
 *   - by: 上传者名；time: 已格式化的 "HH:mm" 字符串（后端 DateTimeFormatter 处理）
 *   - 兼容旧 mock 字段：user / time(时间戳)
 * @prop {Boolean} own 是否本人分享（显示撤回按钮）
 * @emit withdraw(id) 撤回事件
 * @emit user(userId) 点击上传者（跳转用户主页）
 * 每条分享 = (场景, 图片, 关联歌曲) 三元组 —— 数据飞轮源头（docs/03）
 */
const props = defineProps({
  moment: { type: Object, required: true },
  own: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
})

defineEmits(['withdraw', 'user'])
const imagePath = ref('')
let generation = 0
watch(() => props.moment.imageUrl, async (url) => {
  const current = ++generation
  imagePath.value = ''
  if (!url) return
  try { const path = await loadImage(url); if (current === generation) imagePath.value = path }
  catch { /* 图片撤回、不可访问或断网时保留占位 */ }
}, { immediate: true })
onUnmounted(() => { generation++ })
</script>

<style lang="scss" scoped>
.moment-card {
  padding: 14rpx 14rpx 22rpx;
  overflow: hidden;
  border-radius: 20rpx;
  background: #fff;

  &__image {
    width: 100%;
    height: 370rpx;
    display: block;
    border-radius: 12rpx;
  }

  &__body {
    padding: 15rpx 4rpx 0;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 8rpx;
  }

  &__name {
    font-size: $sz-font-xs;
    color: $sz-text;
    font-weight: 600;
    flex: 1;
  }
  /* 撤回按钮：轻量不突出（决议 D6） */
  &__withdraw {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }

  &__text {
    font-size: 24rpx;
    display: block;
    margin-bottom: 10rpx;
    color: $sz-text;
    font-style: italic;
  }

  &__meta {
    display: flex;
    justify-content: space-between;
    margin-top: 4rpx;
  }

  &__track {
    font-size: $sz-font-xs;
    color: $sz-morandi-study;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    min-width: 0;
  }

  &__time {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
    margin-right: 12rpx;
  }
}
.moment-card--compact {
  transform: rotate(-1.5deg);
  padding: 12rpx 12rpx 16rpx;
  .moment-card__image { height: 165rpx; }
  .moment-card__body { padding-top: 12rpx; }
  .moment-card__name, .moment-card__track, .moment-card__time { font-size: 17rpx; }
  .moment-card__meta { display: block; }
  .moment-card__time, .moment-card__track { display: block; }
}
</style>
