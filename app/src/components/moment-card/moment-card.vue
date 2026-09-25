<template>
  <!-- 图片分享卡片 v2：图片与歌曲关联 + 上传者 + 可撤回（本人） -->
  <view class="moment-card sz-card">
    <image
      v-if="moment.imageUrl"
      class="moment-card__image"
      :src="moment.imageUrl"
      mode="aspectFill"
    />
    <view v-else class="moment-card__image" :style="{ backgroundColor: moment.color }" />
    <view class="moment-card__body">
      <view class="moment-card__user" @click.stop="$emit('user', moment.userId)">
        <view class="moment-card__avatar">{{ (moment.by || moment.user || '?')[0].toUpperCase() }}</view>
        <text class="moment-card__name">{{ moment.by || moment.user }}</text>
        <text v-if="own" class="moment-card__withdraw" @click.stop="$emit('withdraw', moment.id)">撤回</text>
      </view>
      <text v-if="moment.text" class="moment-card__text">{{ moment.text }}</text>
      <view class="moment-card__meta">
        <text class="moment-card__track">♪ {{ moment.track }}</text>
        <text class="moment-card__time">{{ moment.time }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
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
defineProps({
  moment: { type: Object, required: true },
  own: { type: Boolean, default: false },
})

defineEmits(['withdraw', 'user'])
</script>

<style lang="scss" scoped>
.moment-card {
  padding: 0;
  overflow: hidden;

  &__image {
    width: 100%;
    height: 200rpx;
    display: block;
  }

  &__body {
    padding: 12rpx 16rpx;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 10rpx;
  }

  &__avatar {
    width: 36rpx;
    height: 36rpx;
    border-radius: 50%;
    background-color: $sz-accent;
    color: #ffffff;
    font-size: $sz-font-xs;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__name {
    font-size: $sz-font-xs;
    color: $sz-text-secondary;
    flex: 1;
  }
  /* 撤回按钮：轻量不突出（决议 D6） */
  &__withdraw {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
  }

  &__text {
    font-size: $sz-font-sm;
    display: block;
    margin-top: 6rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    justify-content: space-between;
    margin-top: 6rpx;
  }

  &__track {
    font-size: $sz-font-xs;
    color: $sz-accent;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    min-width: 0;
  }

  &__time {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
    margin-left: 12rpx;
  }
}
</style>
