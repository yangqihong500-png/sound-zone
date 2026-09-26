<template>
  <!-- 歌单条目 v2：FIFO 位次 + 曲目 + 上传者 + 点赞（仅信号）+ 当前播放高亮 -->
  <view class="queue-item" :class="{ 'queue-item--playing': playing }">
    <text class="queue-item__rank">{{ playing ? '♫' : rank }}</text>
    <view class="queue-item__cover" :class="{ 'queue-item__cover--playing': playing }">
      <image v-if="item.coverUrl" class="queue-item__cover-image" :src="item.coverUrl" mode="aspectFill" />
      <text v-else>♫</text>
    </view>
    <view class="queue-item__info">
      <text class="queue-item__title">{{ item.title }}</text>
      <text class="queue-item__artist" @click.stop="$emit('user', item.userId)">{{ item.artist }} · @{{ item.by }}</text>
      <text v-if="item.attribution" class="queue-item__source">{{ item.attribution }}</text>
    </view>
    <view class="queue-item__like" @click.stop="onLike">
      <text class="queue-item__like-icon" :class="{ liked }">{{ liked ? '♥' : '♡' }}</text>
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
import { ref, watch } from 'vue'
import { likeQueueItem } from '@/api/mock.js'

const props = defineProps({
  item: { type: Object, required: true },
  rank: { type: Number, default: 0 },
  playing: { type: Boolean, default: false },
})

defineEmits(['user'])
const liked = ref(!!props.item.liked)
const count = ref(props.item.likes || 0)
const busy = ref(false)
watch(() => [props.item.likes, props.item.liked], ([likes, active]) => { count.value = likes || 0; liked.value = !!active })
async function onLike() {
  if (busy.value || !props.item.itemId) return
  busy.value = true
  try {
    const result = await likeQueueItem(props.item.itemId, !liked.value)
    liked.value = result.liked
    count.value = result.likes
  } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
  finally { busy.value = false }
}
</script>

<style lang="scss" scoped>
.queue-item {
  display: flex;
  align-items: center;
  gap: $sz-gap-sm;
  padding: 18rpx 8rpx;
  border-bottom: 1rpx solid rgba(0,0,0,.06);
  &:last-child { border-bottom: 0; }

  /* 当前播放：低饱和主题色轻高亮（决议 D3） */
  &--playing {
    background-color: transparent;

    .queue-item__rank,
    .queue-item__title {
      color: $sz-accent;
      font-weight: 500;
    }
  }

  &__rank {
    width: 30rpx;
    font-size: 22rpx;
    color: $sz-text-tertiary;
    text-align: center;
  }
  &__cover {
    width: 70rpx; height: 70rpx; border-radius: 18rpx; flex-shrink: 0;
    overflow: hidden;
    display: flex; align-items: center; justify-content: center;
    background: linear-gradient(140deg, #a8b8c8, #c3b8d9);
    color: rgba(255,255,255,.9); font-size: 26rpx;
    &--playing { box-shadow: 0 0 0 2rpx rgba(168,184,200,.5); }
  }
  &__cover-image { width: 100%; height: 100%; }

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
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__source {
    font-size: 18rpx;
    color: $sz-text-tertiary;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
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
