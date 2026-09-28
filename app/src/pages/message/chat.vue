<template>
  <view class="chat">
    <view class="nav sz-glass" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav__back" @click="goBack">‹</view>
      <view class="nav__peer">
        <text class="nav__name">{{ peer ? '@' + peer.name : 'Messages' }}</text>
        <text class="nav__hint">Private conversation</text>
      </view>
      <view class="nav__back" />
    </view>

    <view v-if="error" class="state">
      <text>{{ error }}</text>
      <button class="sz-btn-secondary" @click="goBack">Back</button>
    </view>

    <scroll-view
      v-else
      class="messages"
      scroll-y
      :scroll-into-view="lastMessageId"
      :scroll-with-animation="true"
    >
      <view class="messages__inner">
        <view v-if="loading" class="empty">Loading…</view>
        <view v-else-if="!messages.length" class="empty">
          <text>Start a quiet conversation.</text>
          <text class="empty__hint">Only you and {{ peer?.name }} can see these messages.</text>
        </view>
        <view
          v-for="message in messages"
          :id="'message-' + message.id"
          :key="message.id"
          class="message"
          :class="{ 'message--mine': message.fromUserId === session.userId }"
        >
          <view class="message__bubble">
            <text class="message__body">{{ message.body }}</text>
            <text class="message__time">{{ formatTime(message.createdAt) }}</text>
          </view>
        </view>
        <view class="messages__bottom" />
      </view>
    </scroll-view>

    <view v-if="!error" class="composer sz-glass">
      <textarea
        v-model="draft"
        class="composer__input"
        maxlength="500"
        auto-height
        :disabled="sending"
        placeholder="Write a message…"
        confirm-type="send"
        @confirm="send"
      />
      <button
        class="composer__send"
        :disabled="!draft.trim() || sending"
        @click="send"
      >Send</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { getConversation, getUserProfile, sendDirectMessage } from '@/api/mock.js'
import { session } from '@/api/session.js'
import { attachMessages, detachMessages } from '@/services/message-session.js'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const peer = ref(null)
const messages = ref([])
const draft = ref('')
const loading = ref(true)
const sending = ref(false)
const error = ref('')
let peerId = null
let loadingMessages = false
let unloaded = false

const lastMessageId = computed(() => {
  const last = messages.value[messages.value.length - 1]
  return last ? `message-${last.id}` : ''
})

onLoad(async (option) => {
  peerId = Number(option.userId)
  if (!Number.isInteger(peerId) || peerId <= 0 || peerId === session.userId) {
    loading.value = false
    error.value = 'Invalid conversation'
    return
  }
  try {
    peer.value = await getUserProfile(peerId)
    await loadMessages()
    if (!unloaded) attachMessages(loadMessages)
  } catch (e) {
    error.value = e.message || 'Could not open this conversation'
  } finally {
    loading.value = false
  }
})

onUnload(() => {
  unloaded = true
  detachMessages()
})

async function loadMessages() {
  if (loadingMessages || unloaded) return
  loadingMessages = true
  try {
    const result = await getConversation(peerId)
    if (!unloaded) messages.value = result
  } catch (e) {
    if (!messages.value.length) error.value = e.message
  } finally {
    loadingMessages = false
  }
}

async function send() {
  const body = draft.value.trim()
  if (!body || sending.value) return
  sending.value = true
  try {
    const message = await sendDirectMessage(peerId, body)
    if (!messages.value.some((item) => item.id === message.id)) messages.value.push(message)
    draft.value = ''
  } catch (e) {
    uni.showToast({ title: e.message || 'Message not sent', icon: 'none' })
  } finally {
    sending.value = false
  }
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.chat {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $sz-bg;
}

.nav {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40rpx 22rpx;
  border-radius: 0;
  background: rgba(255,255,255,.72);

  &__back { width: 60rpx; font-size: 56rpx; line-height: 1; color: $sz-text; }
  &__peer { display: flex; flex-direction: column; align-items: center; min-width: 0; }
  &__name { max-width: 520rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 30rpx; font-weight: 600; }
  &__hint { margin-top: 2rpx; color: $sz-text-tertiary; font-size: 18rpx; }
}

.messages { flex: 1; min-height: 0; }
.messages__inner { padding: 30rpx 28rpx; }
.messages__bottom { height: 12rpx; }

.message {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 18rpx;

  &--mine { justify-content: flex-end; }

  &__bubble {
    max-width: 72%;
    box-sizing: border-box;
    padding: 18rpx 22rpx 12rpx;
    border-radius: 28rpx 28rpx 28rpx 8rpx;
    background: #ffffff;
    box-shadow: 0 8rpx 26rpx rgba(40,45,55,.07);
  }

  &--mine &__bubble {
    border-radius: 28rpx 28rpx 8rpx 28rpx;
    background: $sz-primary;
    color: #ffffff;
  }

  &__body { display: block; word-break: break-word; font-size: 26rpx; line-height: 1.45; }
  &__time { display: block; margin-top: 7rpx; text-align: right; font-size: 17rpx; color: $sz-text-tertiary; }
  &--mine &__time { color: rgba(255,255,255,.66); }
}

.empty {
  min-height: 50vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  color: $sz-text-secondary;
  font-size: 25rpx;

  &__hint { color: $sz-text-tertiary; font-size: 20rpx; }
}

.state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 22rpx;
  color: $sz-text-secondary;

  button { margin: 0; padding: 8rpx 40rpx; border-radius: 999rpx; font-size: 23rpx; }
}

.composer {
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
  padding: 18rpx 28rpx calc(18rpx + env(safe-area-inset-bottom));
  border-radius: 0;
  background: rgba(255,255,255,.78);

  &__input {
    flex: 1;
    max-height: 180rpx;
    box-sizing: border-box;
    padding: 17rpx 22rpx;
    border-radius: 26rpx;
    background: rgba(0,0,0,.055);
    font-size: 25rpx;
    line-height: 1.35;
  }

  &__send {
    flex-shrink: 0;
    margin: 0;
    padding: 10rpx 28rpx;
    border-radius: 999rpx;
    background: $sz-primary;
    color: #ffffff;
    font-size: 23rpx;
    font-weight: 600;

    &[disabled] { background: rgba(0,0,0,.08); color: $sz-text-tertiary; opacity: 1; }
    &::after { border: none; }
  }
}
</style>
