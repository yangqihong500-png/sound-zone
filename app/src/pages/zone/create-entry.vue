<template>
  <view class="entry">
    <text class="entry__text">Opening Create Zone…</text>
  </view>
</template>

<script setup>
import { onShow } from '@dcloudio/uni-app'
import { beginCreateFromTab, finishCreateFromTab, isCreateFromTabActive } from '@/services/create-entry.js'

function goHome() {
  uni.switchTab({
    url: '/pages/index/index',
    fail: () => uni.reLaunch({ url: '/pages/index/index' }),
  })
}

onShow(() => {
  // 系统返回手势可能直接退回这个 tab 入口；此时回 Home，避免再次打开表单。
  if (isCreateFromTabActive()) {
    finishCreateFromTab()
    goHome()
    return
  }
  if (!beginCreateFromTab()) return
  setTimeout(() => {
    uni.navigateTo({
      url: '/pages/zone/create',
      fail: () => {
        finishCreateFromTab()
        goHome()
      },
    })
  }, 0)
})
</script>

<style lang="scss" scoped>
.entry {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $sz-bg;

  &__text {
    color: $sz-text-tertiary;
    font-size: $sz-font-sm;
  }
}
</style>
