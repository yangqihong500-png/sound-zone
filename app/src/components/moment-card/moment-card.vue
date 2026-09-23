<template>
  <!-- 碎片卡片：图片占位 + 文案 + 自动绑定的配乐（数据飞轮的三元组） -->
  <view class="moment-card">
    <view class="moment-card__image" :style="{ backgroundColor: moment.color }" />
    <view class="moment-card__body">
      <text class="moment-card__text">{{ moment.text }}</text>
      <view class="moment-card__meta">
        <text class="moment-card__track">♪ {{ moment.track }}</text>
        <text class="moment-card__time">{{ moment.time }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * MomentCard 碎片卡片组件
 * @prop {Object} moment { id, text, track, time, color }
 * 每条碎片 = (场景, 图/文, 配乐) 三元组 —— 数据飞轮的源头（docs/03）
 * Demo 阶段图片用色块占位，真实版本接腾讯云 COS
 */
defineProps({
  moment: { type: Object, required: true },
})
</script>

<style lang="scss" scoped>
.moment-card {
  background-color: $sz-card;
  border-radius: $sz-radius-md;
  overflow: hidden;

  &__image {
    width: 100%;
    height: 180rpx;
  }

  &__body {
    padding: 12rpx 16rpx;
  }

  &__text {
    font-size: $sz-font-sm;
    display: block;
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
    color: $sz-primary-dark;
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
