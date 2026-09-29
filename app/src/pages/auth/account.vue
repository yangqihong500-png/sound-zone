<template>
  <view class="page">
    <view class="nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <text class="nav__back" @click="goBack">‹</text>
      <text class="nav__title">Account</text>
      <view class="nav__back" />
    </view>

    <view class="panel">
      <text class="panel__title">{{ mode === 'login' ? 'Welcome back' : 'Create your account' }}</text>
      <text class="panel__hint">{{ mode === 'login' ? 'Sign in to keep your SoundZone identity.' : 'Your current demo activity will stay with this account.' }}</text>

      <view class="tabs">
        <view class="tabs__item" :class="{ 'tabs__item--active': mode === 'login' }" @click="mode = 'login'">Log in</view>
        <view class="tabs__item" :class="{ 'tabs__item--active': mode === 'register' }" @click="mode = 'register'">Register</view>
      </view>

      <view class="field">
        <text class="field__label">Username</text>
        <input v-model="username" class="field__input" maxlength="32" placeholder="2–32 characters" autocomplete="username" />
      </view>
      <view class="field">
        <text class="field__label">Password</text>
        <input v-model="password" class="field__input" maxlength="72" placeholder="At least 8 characters" password autocomplete="current-password" @confirm="submit" />
      </view>

      <button class="submit" :disabled="busy || !ready" @click="submit">{{ busy ? 'Please wait…' : (mode === 'login' ? 'Log in' : 'Create account') }}</button>
      <text class="privacy">Passwords are stored as salted PBKDF2 hashes. Production deployment must use HTTPS.</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { loginAccount, registerAccount } from '@/api/session.js'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const mode = ref('login')
const username = ref('')
const password = ref('')
const busy = ref(false)
const ready = computed(() => username.value.trim().length >= 2 && password.value.length >= 8)

async function submit() {
  if (busy.value || !ready.value) return
  busy.value = true
  try {
    if (mode.value === 'login') await loginAccount(username.value.trim(), password.value)
    else await registerAccount(username.value.trim(), password.value)
    uni.showToast({ title: mode.value === 'login' ? 'Logged in' : 'Account created', icon: 'success' })
    setTimeout(goBack, 250)
  } catch (e) { uni.showToast({ title: e.message, icon: 'none' }) }
  finally { busy.value = false }
}
function goBack() {
  if (getCurrentPages().length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/user/index' })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; box-sizing: border-box; background: $sz-bg; }
.nav { display: flex; align-items: center; justify-content: space-between; padding: 0 36rpx 24rpx; background: rgba(255,255,255,.82); border-bottom: 1rpx solid rgba(0,0,0,.07); }
.nav__back { width: 54rpx; font-size: 56rpx; line-height: 1; color: $sz-text; }
.nav__title { font-size: 33rpx; font-weight: 600; }
.panel { margin: 56rpx 32rpx; padding: 44rpx 36rpx; border-radius: 38rpx; background: #fff; box-shadow: $sz-shadow-soft; }
.panel__title { display: block; font-size: 38rpx; font-weight: 650; color: $sz-text; }
.panel__hint { display: block; margin-top: 8rpx; color: $sz-text-tertiary; font-size: 22rpx; }
.tabs { display: flex; margin-top: 36rpx; padding: 6rpx; border-radius: 20rpx; background: rgba(0,0,0,.055); }
.tabs__item { flex: 1; padding: 15rpx 0; border-radius: 16rpx; text-align: center; color: $sz-text-secondary; font-size: 24rpx; }
.tabs__item--active { background: #fff; color: $sz-text; font-weight: 600; box-shadow: 0 4rpx 12rpx rgba(0,0,0,.08); }
.field { margin-top: 30rpx; }
.field__label { display: block; margin-bottom: 9rpx; font-size: 21rpx; color: $sz-text-secondary; }
.field__input { box-sizing: border-box; width: 100%; padding: 20rpx 22rpx; border: 1rpx solid $sz-control-border; border-radius: 18rpx; background: $sz-control-soft; font-size: 27rpx; }
.submit { margin-top: 40rpx; border-radius: 999rpx; background: $sz-brand; color: #fff; font-size: 27rpx; font-weight: 600; }
.submit[disabled] { background: rgba(0,0,0,.08); color: $sz-text-tertiary; }
.privacy { display: block; margin-top: 20rpx; color: $sz-text-tertiary; font-size: 19rpx; text-align: center; }
</style>
