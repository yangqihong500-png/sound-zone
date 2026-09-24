<template>
  <view class="page">
    <view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <text class="nav__title">动态</text>
      <view class="nav__back" />
    </view>

    <!-- 半小时内图片流（时间倒序，决议 D6/D9） -->
    <scroll-view class="page__body" scroll-y>
      <text class="window-hint">展示半小时内上传的图片</text>
      <view v-for="m in moments" :key="m.id" class="feed-item">
        <moment-card :moment="m" :own="m.userId === 'me'" @withdraw="onWithdraw" />
        <!-- emoji 轻互动（爱心/大笑/点赞，决议 D7） -->
        <view class="emoji-row">
          <text
            v-for="e in emojis"
            :key="e.name"
            class="emoji-row__item"
            :class="{ 'emoji-row__item--active': m._react === e.name }"
            @click="onReact(m, e.name)"
          >{{ e.icon }}</text>
        </view>
      </view>
      <view v-if="!moments.length" class="empty">半小时内还没有图片分享</view>
      <view class="bottom-spacer" />
    </scroll-view>
  </view>
</template>

<script setup>
/**
 * 动态详情页 v2：2026-09-24 决议 D6/D7/D9
 * 半小时内图片流（时间倒序）+ 本人可撤回 + emoji 轻互动，无评论区
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getMomentFeed, withdrawMoment } from '@/api/mock.js'
import MomentCard from '@/components/moment-card/moment-card.vue'

const moments = ref([])
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const emojis = [
  { name: 'heart', icon: '♡' },
  { name: 'laugh', icon: '☺' },
  { name: 'like', icon: '👍' },
]

let zoneId = null

onLoad(async (option) => {
  zoneId = Number(option.id)
  moments.value = await getMomentFeed(zoneId)
})

function onReact(m, name) {
  // 轻互动：本地态高亮，正式版走 WS 广播（决议 D7）
  m._react = m._react === name ? null : name
}

async function onWithdraw(momentId) {
  await withdrawMoment(zoneId, momentId)
  moments.value = moments.value.filter((m) => m.id !== momentId)
  uni.showToast({ title: '已撤回', icon: 'none' })
}

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;

  &__body {
    flex: 1;
    padding: 0 $sz-gap-md;
  }
}

.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
  padding-left: $sz-gap-md;
  padding-right: $sz-gap-md;

  &__back {
    font-size: 56rpx;
    line-height: 1;
    color: $sz-text;
    width: 60rpx;
  }

  &__title {
    font-size: $sz-font-lg;
    font-weight: 500;
  }
}

.window-hint {
  font-size: $sz-font-xs;
  color: $sz-text-tertiary;
  display: block;
  padding: $sz-gap-sm 8rpx;
}

.feed-item {
  margin-bottom: $sz-gap-md;
}

/* emoji 互动行：轻量不突出 */
.emoji-row {
  display: flex;
  gap: $sz-gap-lg;
  padding: 12rpx 16rpx 0;

  &__item {
    font-size: 32rpx;
    color: $sz-text-tertiary;

    &--active {
      color: $sz-accent;
    }
  }
}

.empty {
  text-align: center;
  color: $sz-text-tertiary;
  font-size: $sz-font-sm;
  padding: 80rpx 0;
}

.bottom-spacer {
  height: 80rpx;
}
</style>
