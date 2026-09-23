<template>
  <!-- 队列条目：排名 + 曲目 + 点歌人 + 点赞 -->
  <view class="queue-item">
    <text class="queue-item__rank">{{ item.rank }}</text>
    <view class="queue-item__info">
      <text class="queue-item__title">{{ item.title }}</text>
      <text class="queue-item__artist">{{ item.artist }} · @{{ item.by }} 点播</text>
    </view>
    <view class="queue-item__like" @click.stop="onLike">
      <text class="queue-item__like-icon" :class="{ liked }">▲</text>
      <text class="queue-item__like-count" :class="{ liked }">{{ count }}</text>
    </view>
  </view>
</template>

<script setup>
/**
 * QueueItem 队列条目组件
 * @prop {Object} item { rank, title, artist, likes, by }
 * 点赞为本地态演示；真实实现走 WS：点赞 → 服务端重算队列得分 → 广播
 * （队列得分公式见 docs/02：点赞×2 + 域主加成×3 − 刷屏惩罚×1.5）
 */
import { ref } from 'vue'

const props = defineProps({
  item: { type: Object, required: true },
})

const liked = ref(false)
const count = ref(props.item.likes)

function onLike() {
  liked.value = !liked.value
  count.value += liked.value ? 1 : -1
}
</script>

<style lang="scss" scoped>
.queue-item {
  display: flex;
  align-items: center;
  gap: $sz-gap-sm;
  padding: $sz-gap-sm 0;
  border-bottom: 1rpx solid $sz-bg;

  &:last-child {
    border-bottom: none;
  }

  &__rank {
    width: 48rpx;
    font-size: $sz-font-lg;
    font-weight: 500;
    color: $sz-text-tertiary;
    text-align: center;
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

  &__like {
    display: flex;
    align-items: center;
    gap: 6rpx;
    padding: 8rpx 16rpx;
    border-radius: 999rpx;
    background-color: $sz-bg;
  }

  &__like-icon {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;

    &.liked {
      color: $sz-primary;
    }
  }

  &__like-count {
    font-size: $sz-font-xs;
    color: $sz-text-secondary;

    &.liked {
      color: $sz-primary;
    }
  }
}
</style>
