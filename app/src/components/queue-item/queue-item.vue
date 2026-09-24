<template>
  <!-- 歌单条目 v2：FIFO 位次 + 曲目 + 上传者 + 点赞（仅信号）+ 当前播放高亮 -->
  <view class="queue-item" :class="{ 'queue-item--playing': playing }">
    <text class="queue-item__rank">{{ playing ? '♪' : rank }}</text>
    <view class="queue-item__info">
      <text class="queue-item__title">{{ item.title }}</text>
      <text class="queue-item__artist">{{ item.artist }} · @{{ item.by }} 上传</text>
    </view>
    <view class="queue-item__like" @click.stop="onLike">
      <text class="queue-item__like-icon" :class="{ liked }">♡</text>
      <text class="queue-item__like-count" :class="{ liked }">{{ count }}</text>
    </view>
  </view>
</template>

<script setup>
/**
 * QueueItem 歌单条目组件（v2：2026-09-24 会议）
 * @prop {Object} item { itemId, title, artist, likes, by }
 * @prop {Number} rank FIFO 等待位次（按上传顺序，决议 D3）
 * @prop {Boolean} playing 是否当前播放（高亮，决议 D3）
 * 点赞为互动信号，不再影响播放顺序
 */
import { ref } from 'vue'

const props = defineProps({
  item: { type: Object, required: true },
  rank: { type: Number, default: 0 },
  playing: { type: Boolean, default: false },
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
  padding: $sz-gap-sm $sz-gap-sm;
  border-radius: $sz-radius-sm;

  /* 当前播放：低饱和主题色轻高亮（决议 D3） */
  &--playing {
    background-color: rgba(140, 155, 171, 0.12);

    .queue-item__rank,
    .queue-item__title {
      color: $sz-accent;
      font-weight: 500;
    }
  }

  &__rank {
    width: 48rpx;
    font-size: $sz-font-base;
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
  }

  &__like-icon {
    font-size: $sz-font-sm;
    color: $sz-text-tertiary;

    &.liked {
      color: $sz-accent;
    }
  }

  &__like-count {
    font-size: $sz-font-xs;
    color: $sz-text-secondary;

    &.liked {
      color: $sz-accent;
    }
  }
}
</style>
