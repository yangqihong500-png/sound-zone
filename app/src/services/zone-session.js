import { reactive } from 'vue'
import { API_BASE } from '@/api/constants.js'
import { token } from '@/api/session.js'
import { getZoneDetail, heartbeat, leaveZone } from '@/api/mock.js'
import {
  playback,
  resetPlayer,
  setPlaybackActivityHandler,
  setPlaybackEndedHandler,
  syncPlayer,
} from './player.js'

/**
 * 跨页面保留的域播放摘要。只有用户主动选择“悬挂域”后才展示全局迷你播放条，
 * 但底层会话从进入域起就由本模块统一维护，避免页面卸载后下一首无法衔接。
 */
export const suspendedZone = reactive({
  active: false,
  zoneId: null,
  name: '',
  coverUrl: '',
  trackTitle: '',
  artist: '',
})

let socket = null
let reconnectTimer = null
let heartbeatTimer = null
let refreshTimer = null
let transitionTimer = null
let currentId = null
let currentSnapshot = null
let callbacks = null
let reconnectDelay = 1000
let generation = 0
let refreshing = null
let lastHeartbeatAt = 0

export function attachZone(id, handlers, snapshot = null) {
  if (currentId && currentId !== id) leaveCurrentZone()
  detachTransport()
  refreshing = null
  currentId = id
  currentSnapshot = snapshot || currentSnapshot
  callbacks = handlers
  suspendedZone.active = false
  updateSummary(currentSnapshot)
  const version = ++generation
  connect(version)
  setPlaybackEndedHandler(onTrackEnded)
  setPlaybackActivityHandler(() => sendHeartbeat(false))
  sendHeartbeat(true)
  heartbeatTimer = setInterval(() => sendHeartbeat(true), 20000)
  // WS 断线兜底及时钟校准；刷新和换曲不依赖域详情页继续存在。
  refreshTimer = setInterval(() => refreshActiveZone().catch(() => {}), 10000)
}

/** 页面重新进入已悬挂的域时不重复 join，只恢复视图订阅并读取权威快照。 */
export async function resumeZone(id, handlers) {
  if (currentId !== id) return null
  attachZone(id, handlers, currentSnapshot)
  return refreshActiveZone()
}

/** 页面卸载后仅解除视图回调，后台音频、心跳、WS 和换曲继续运行。 */
export function detachZoneView() {
  callbacks = null
}

export function suspendCurrentZone(zone) {
  if (!currentId || Number(zone?.id) !== currentId) return false
  currentSnapshot = zone
  callbacks = null
  suspendedZone.active = true
  updateSummary(zone)
  return true
}

export function isZoneSuspended(id = currentId) {
  return suspendedZone.active && currentId === Number(id)
}

export async function refreshActiveZone() {
  if (!currentId) return null
  if (refreshing) return refreshing
  const id = currentId
  const version = generation
  const request = (async () => {
    const started = Date.now()
    try {
      const data = await getZoneDetail(id)
      if (id !== currentId || version !== generation) return null
      currentSnapshot = data
      updateSummary(data)
      await syncPlayer(data, started)
      if (id !== currentId || version !== generation) return null
      callbacks?.changed?.(data)
      return data
    } catch (error) {
      if ([1002, 1003, 2001, 3004].includes(error.code)) endSession(error.message)
      throw error
    }
  })()
  refreshing = request
  try { return await request }
  finally { if (refreshing === request) refreshing = null }
}

function connect(version) {
  if (version !== generation || !currentId) return
  socket = uni.connectSocket({ url: API_BASE.replace(/^http/, 'ws') + '/ws/zones', complete: () => {} })
  socket.onOpen(() => {
    if (version !== generation) return
    reconnectDelay = 1000
    socket.send({ data: JSON.stringify({ zoneId: currentId, token: token() }) })
  })
  socket.onMessage(({ data }) => {
    if (version !== generation) return
    try {
      const event = JSON.parse(data)
      if (event.type === 'ENDED' || event.type === 'ERROR') endSession('域已结束或需要重新进入')
      else if (event.type === 'GLOW') callbacks?.glow?.(event.itemId)
      else refreshActiveZone().catch(() => {})
    } catch { /* 非 JSON 或旧协议消息等待下一次权威快照 */ }
  })
  socket.onClose(() => {
    if (version !== generation || !currentId) return
    reconnectTimer = setTimeout(() => connect(version), reconnectDelay)
    reconnectDelay = Math.min(30000, reconnectDelay * 2)
  })
  socket.onError(() => { /* close 回调负责重连，定时快照仍可用 */ })
}

async function sendHeartbeat(force) {
  const id = currentId
  if (!id) return
  const now = Date.now()
  // BackgroundAudioManager 的进度事件可在锁屏时继续触发，用它补偿被系统节流的 JS 定时器。
  if (!force && now - lastHeartbeatAt < 15000) return
  lastHeartbeatAt = now
  try {
    await heartbeat(id, playback.itemId, playback.playing)
  } catch (error) {
    if (id === currentId && [1002, 1003, 3004].includes(error.code)) endSession(error.message)
  }
}

async function onTrackEnded() {
  const endedItemId = playback.itemId
  try {
    const data = await refreshActiveZone()
    // 音频文件时长与曲库元数据可能有不足一秒误差，再读取一次避免停在尾帧。
    if (currentId && data?.nowPlaying?.itemId === endedItemId) {
      clearTimeout(transitionTimer)
      transitionTimer = setTimeout(() => refreshActiveZone().catch(() => {}), 1200)
    }
  } catch { /* 权威刷新会自行处理域结束，其余网络错误等待定时重试 */ }
}

function updateSummary(zone) {
  if (!zone || !currentId) return
  Object.assign(suspendedZone, {
    zoneId: currentId,
    name: zone.name || 'SoundZone',
    coverUrl: zone.nowPlaying?.coverUrl || zone.coverUrl || '',
    trackTitle: zone.nowPlaying?.title || '等待下一首歌',
    artist: zone.nowPlaying?.artist || '',
  })
}

function endSession(message) {
  const ended = callbacks?.ended
  leaveCurrentZone()
  ended?.(message || '域已结束')
}

function detachTransport() {
  clearInterval(heartbeatTimer)
  clearInterval(refreshTimer)
  clearTimeout(reconnectTimer)
  clearTimeout(transitionTimer)
  const old = socket
  socket = null
  generation++
  if (old) old.close({})
}

export function leaveCurrentZone() {
  const id = currentId
  currentId = null
  currentSnapshot = null
  callbacks = null
  refreshing = null
  lastHeartbeatAt = 0
  detachTransport()
  setPlaybackEndedHandler(null)
  setPlaybackActivityHandler(null)
  resetPlayer()
  Object.assign(suspendedZone, {
    active: false,
    zoneId: null,
    name: '',
    coverUrl: '',
    trackTitle: '',
    artist: '',
  })
  if (id) return leaveZone(id).catch(() => {}) // 断网由服务端心跳宽限收口
  return Promise.resolve()
}

export function activeZoneId() { return currentId }
