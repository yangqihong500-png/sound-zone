<template>
  <view class="page">
    <!-- 顶部返回（决议 D9：简洁表单感） -->
    <view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <text class="nav__title">创建新域</text>
      <view class="nav__back" />
    </view>

    <scroll-view class="page__body" scroll-y>
      <!-- 主题输入：极简横线 -->
      <view class="field">
        <view class="field__line">
          <input
            class="field__input"
            v-model="form.name"
            placeholder="输入域主题"
            placeholder-class="placeholder"
            maxlength="30"
          />
        </view>
      </view>

      <!-- 场景选择（归入分发场景） -->
      <view class="section">
        <text class="section__label">场景</text>
        <view class="capsules">
          <view
            v-for="s in sceneOptions"
            :key="s"
            class="capsule"
            :class="{ 'capsule--active': form.scene === s }"
            @click="form.scene = s"
          >{{ s }}</view>
        </view>
      </view>

      <!-- 主题标签：五类分组（决议 D5） -->
      <view class="section">
        <text class="section__label">主题标签</text>
        <view v-for="(tags, category) in tagCatalog" :key="category" class="tag-group">
          <text class="tag-group__name">{{ category }}</text>
          <view class="capsules">
            <view
              v-for="tag in tags"
              :key="tag"
              class="capsule"
              :class="{ 'capsule--active': form.tags.includes(tag) }"
              @click="toggleTag(form.tags, tag)"
            >{{ tag }}</view>
          </view>
        </view>
      </view>

      <!-- 音乐过滤设置（决议 D5：双模式，同页完成不用弹窗） -->
      <view class="section">
        <text class="section__label">音乐过滤</text>
        <view class="mode-switch">
          <view
            class="mode-switch__option"
            :class="{ 'mode-switch__option--active': form.filterMode === 'BAN' }"
            @click="form.filterMode = 'BAN'"
          >禁止含这些标签的音乐</view>
          <view
            class="mode-switch__option"
            :class="{ 'mode-switch__option--active': form.filterMode === 'ALLOW' }"
            @click="form.filterMode = 'ALLOW'"
          >仅允许含这些标签的音乐</view>
        </view>
        <view class="capsules capsules--filter">
          <view
            v-for="tag in allTags"
            :key="tag"
            class="capsule"
            :class="{ 'capsule--active': form.filterTags.includes(tag) }"
            @click="toggleTag(form.filterTags, tag)"
          >{{ tag }}</view>
        </view>
      </view>

      <!-- 私密设置（决议 D2：克制的控件，不用强警示视觉） -->
      <view class="section">
        <view class="privacy-row">
          <text class="section__label">私密域</text>
          <switch :checked="form.visibility === 'PRIVATE'" @change="onPrivacyChange" color="#8c9bab" />
        </view>
        <view v-if="form.visibility === 'PRIVATE'" class="field">
          <view class="field__line">
            <input
              class="field__input"
              v-model="form.password"
              placeholder="设置密码（留空则仅邀请链接可进）"
              placeholder-class="placeholder"
              maxlength="16"
            />
          </view>
          <text class="field__hint">创建后自动生成邀请链接，可分享给指定的人</text>
        </view>
      </view>

      <!-- 初始歌单（建域门槛：≥3 首，docs/02 第 1 步） -->
      <view class="section">
        <text class="section__label">初始歌单（至少 3 首）</text>
        <view class="capsules">
          <view
            v-for="t in seedTracks"
            :key="t.id"
            class="capsule"
            :class="{ 'capsule--active': form.trackIds.includes(t.id) }"
            @click="toggleTrack(t.id)"
          >{{ t.title }}</view>
        </view>
        <text class="field__hint">已选 {{ form.trackIds.length }} 首</text>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>

    <!-- 底部悬浮创建按钮（表单完成后高亮，决议 D9） -->
    <view class="create-bar">
      <button
        class="create-bar__btn"
        :class="{ 'create-bar__btn--ready': canCreate }"
        @click="onCreate"
      >创建域</button>
    </view>
  </view>
</template>

<script setup>
/**
 * 创建域页 v2：2026-09-24 决议 D2/D5/D9
 * 结构：主题输入 → 场景 → 五类主题标签 → 过滤双模式 → 私密设置 → 创建按钮
 * 提交后跳转域详情（初始歌单选择属后续迭代，Demo 以空队列开局）
 */
import { ref, computed, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { SCENES, TAG_CATALOG, createZone, searchTracks } from '@/api/mock.js'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)

const tagCatalog = TAG_CATALOG
const sceneOptions = SCENES.filter((s) => s !== '全部')
const allTags = Object.values(TAG_CATALOG).flat()

// 初始歌单候选曲目（简化：展示曲库前若干首，用户点选）
const seedTracks = ref([])

onLoad(async () => {
  try {
    seedTracks.value = await searchTracks('')
  } catch (e) {
    seedTracks.value = []
  }
})

const form = reactive({
  name: '',
  scene: '',
  tags: [],
  filterMode: 'BAN',
  filterTags: [],
  visibility: 'PUBLIC',
  password: '',
  trackIds: [],
})

const canCreate = computed(() =>
  form.name.trim().length > 0 && form.scene && form.trackIds.length >= 3
)

function toggleTag(list, tag) {
  const i = list.indexOf(tag)
  i >= 0 ? list.splice(i, 1) : list.push(tag)
}

function toggleTrack(id) {
  const i = form.trackIds.indexOf(id)
  i >= 0 ? form.trackIds.splice(i, 1) : form.trackIds.push(id)
}

function onPrivacyChange(e) {
  form.visibility = e.detail.value ? 'PRIVATE' : 'PUBLIC'
}

async function onCreate() {
  if (!canCreate.value) {
    uni.showToast({ title: '请填写主题、场景并至少选 3 首歌', icon: 'none' })
    return
  }
  try {
    const zone = await createZone({
      name: form.name.trim(),
      scene: form.scene,
      tags: form.tags,
      filterMode: form.filterMode,
      filterTags: form.filterTags,
      visibility: form.visibility,
      password: form.password || null,
      trackIds: form.trackIds,
    })
    uni.showToast({ title: '域已创建', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/zone/detail?id=${zone.id}` })
    }, 600)
  } catch (e) {
    uni.showToast({ title: e.message || '创建失败', icon: 'none' })
  }
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

.field {
  margin-top: $sz-gap-md;

  &__line {
    border-bottom: 1rpx solid rgba(0, 0, 0, 0.15);
    padding: 16rpx 4rpx;
  }

  &__input {
    font-size: $sz-font-lg;
  }

  &__hint {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
    margin-top: 8rpx;
    display: block;
  }
}

.placeholder {
  color: $sz-text-tertiary;
}

.section {
  margin-top: $sz-gap-lg;

  &__label {
    font-size: $sz-font-sm;
    color: $sz-text-secondary;
    display: block;
    margin-bottom: $sz-gap-sm;
  }
}

.tag-group {
  margin-bottom: $sz-gap-sm;

  &__name {
    font-size: $sz-font-xs;
    color: $sz-text-tertiary;
    display: block;
    margin-bottom: 8rpx;
  }
}

/* 低饱和胶囊标签，选中主题色轻高亮 */
.capsules {
  display: flex;
  flex-wrap: wrap;
  gap: $sz-gap-sm;

  &--filter {
    margin-top: $sz-gap-sm;
  }
}

.capsule {
  font-size: $sz-font-sm;
  color: $sz-text-secondary;
  padding: 10rpx 28rpx;
  border-radius: 999rpx;
  background-color: rgba(0, 0, 0, 0.04);

  &--active {
    background-color: $sz-accent;
    color: #ffffff;
  }
}

/* 过滤双模式切换：极简分段控件 */
.mode-switch {
  display: flex;
  gap: $sz-gap-sm;

  &__option {
    flex: 1;
    text-align: center;
    font-size: $sz-font-sm;
    color: $sz-text-secondary;
    padding: 16rpx 0;
    border-radius: $sz-radius-md;
    background-color: rgba(0, 0, 0, 0.04);

    &--active {
      background-color: $sz-primary;
      color: #ffffff;
    }
  }
}

.privacy-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bottom-spacer {
  height: 160rpx;
}

.create-bar {
  padding: $sz-gap-sm $sz-gap-md calc(#{$sz-gap-sm} + env(safe-area-inset-bottom));

  &__btn {
    font-size: $sz-font-base;
    border-radius: 999rpx;
    background-color: rgba(0, 0, 0, 0.08);
    color: $sz-text-tertiary;

    &::after {
      border: none;
    }

    &--ready {
      background-color: $sz-primary;
      color: #ffffff;
      font-weight: 500;
    }
  }
}
</style>
