<template>
  <view class="page">
    <view v-if="loading" class="state">正在加载共听记录…</view>
    <template v-else>
      <view class="hero">
        <text class="hero__eyebrow">ALL-TIME CO-LISTENING</text>
        <text class="hero__value">{{ formatLong(summary.totalSeconds) }}</text>
        <text class="hero__caption">在 SoundZone 里与大家同步收听的时间</text>
      </view>

      <view class="overview">
        <view class="overview__item">
          <text class="overview__value">{{ formatCompact(summary.todaySeconds) }}</text>
          <text class="overview__label">Today</text>
        </view>
        <view class="overview__divider" />
        <view class="overview__item">
          <text class="overview__value">{{ formatCompact(summary.last7DaysSeconds) }}</text>
          <text class="overview__label">Last 7 days</text>
        </view>
      </view>

      <view class="panel">
        <view class="panel__header">
          <text class="panel__title">Last 7 days</text>
          <text class="panel__total">{{ formatCompact(summary.last7DaysSeconds) }}</text>
        </view>
        <view class="chart">
          <view v-for="day in summary.daily" :key="day.date" class="chart__day">
            <view class="chart__track">
              <view class="chart__bar" :style="{ height: barHeight(day.seconds) }" />
            </view>
            <text class="chart__value">{{ shortValue(day.seconds) }}</text>
            <text class="chart__label">{{ dayLabel(day.date) }}</text>
          </view>
        </view>
      </view>

      <view v-if="summary.topZones?.length" class="panel top-zones">
        <text class="panel__title">Most listened zones</text>
        <view v-for="(zone, index) in summary.topZones" :key="zone.zoneId" class="zone-row">
          <text class="zone-row__rank">{{ index + 1 }}</text>
          <text class="zone-row__name">{{ zone.name }}</text>
          <text class="zone-row__time">{{ formatCompact(zone.seconds) }}</text>
        </view>
      </view>

      <text class="privacy-note">只统计域内正在同步播放且由服务端确认的有效共听时间</text>
    </template>
    <suspended-zone-player />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getListeningSummary } from '@/api/mock.js'

const loading = ref(true)
const summary = ref({ totalSeconds: 0, todaySeconds: 0, last7DaysSeconds: 0, daily: [], topZones: [] })
const chartMax = computed(() => Math.max(1, ...summary.value.daily.map((day) => day.seconds || 0)))

onShow(load)

async function load() {
  loading.value = true
  try { summary.value = await getListeningSummary() }
  catch (e) { uni.showToast({ title: e.message || '共听记录加载失败', icon: 'none' }) }
  finally { loading.value = false }
}

function formatCompact(seconds = 0) {
  const minutes = Math.floor(Math.max(0, seconds) / 60)
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours}h ${rest}m` : `${hours}h`
}
function formatLong(seconds = 0) {
  const minutes = Math.floor(Math.max(0, seconds) / 60)
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours} hr ${rest} min` : `${hours} hr`
}
function barHeight(seconds) {
  if (!seconds) return '6rpx'
  return `${Math.max(12, Math.round(seconds / chartMax.value * 150))}rpx`
}
function shortValue(seconds) {
  const minutes = Math.floor(Math.max(0, seconds) / 60)
  if (!minutes) return '0'
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  return `${hours}h`
}
function dayLabel(date) {
  const parts = String(date).split('-').map(Number)
  if (parts.length !== 3) return date
  const day = new Date(parts[0], parts[1] - 1, parts[2]).getDay()
  return ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][day]
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 32rpx 32rpx 100rpx; background: $sz-bg; }
.state { padding: 120rpx 0; text-align: center; color: $sz-text-tertiary; font-size: 24rpx; }
.hero { min-height: 320rpx; box-sizing: border-box; padding: 46rpx 36rpx; border-radius: 44rpx; display: flex; flex-direction: column; justify-content: center; background: linear-gradient(145deg, #191a1d, #424750); color: #fff; box-shadow: $sz-shadow-float; }
.hero__eyebrow { font-size: 18rpx; letter-spacing: 3rpx; opacity: .64; }
.hero__value { margin-top: 18rpx; font-size: 64rpx; line-height: 1.05; font-weight: 650; }
.hero__caption { margin-top: 20rpx; font-size: 21rpx; opacity: .7; }
.overview { display: flex; align-items: center; margin-top: 24rpx; padding: 28rpx 0; border-radius: 32rpx; background: #fff; box-shadow: $sz-shadow-soft; }
.overview__item { flex: 1; display: flex; flex-direction: column; align-items: center; }
.overview__divider { width: 1rpx; height: 58rpx; background: rgba(0,0,0,.08); }
.overview__value { color: $sz-text; font-size: 34rpx; font-weight: 600; }
.overview__label { margin-top: 4rpx; color: $sz-text-tertiary; font-size: 20rpx; }
.panel { margin-top: 24rpx; padding: 30rpx; border-radius: 32rpx; background: #fff; box-shadow: $sz-shadow-soft; }
.panel__header { display: flex; align-items: center; justify-content: space-between; }
.panel__title { color: $sz-text; font-size: 28rpx; font-weight: 600; }
.panel__total { color: $sz-text-secondary; font-size: 23rpx; }
.chart { height: 236rpx; margin-top: 24rpx; display: flex; align-items: flex-end; gap: 10rpx; }
.chart__day { flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: center; }
.chart__track { height: 150rpx; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.chart__bar { width: 34rpx; min-height: 6rpx; border-radius: 18rpx 18rpx 8rpx 8rpx; background: linear-gradient(180deg, #596372, #1c1c1e); }
.chart__value { margin-top: 8rpx; color: $sz-text-secondary; font-size: 16rpx; white-space: nowrap; }
.chart__label { margin-top: 4rpx; color: $sz-text-tertiary; font-size: 17rpx; }
.top-zones { padding-bottom: 12rpx; }
.zone-row { display: flex; align-items: center; min-height: 84rpx; border-bottom: 1rpx solid rgba(0,0,0,.06); }
.zone-row:last-child { border-bottom: 0; }
.zone-row__rank { width: 50rpx; color: $sz-text-tertiary; font-size: 22rpx; }
.zone-row__name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: $sz-text; font-size: 25rpx; }
.zone-row__time { margin-left: 18rpx; color: $sz-text-secondary; font-size: 22rpx; }
.privacy-note { display: block; margin: 28rpx 24rpx 0; text-align: center; color: $sz-text-tertiary; font-size: 19rpx; line-height: 1.6; }
</style>
