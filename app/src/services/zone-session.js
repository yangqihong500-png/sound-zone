import { API_BASE } from '@/api/constants.js'
import { token } from '@/api/session.js'
import { heartbeat, leaveZone } from '@/api/mock.js'
import { playback, resetPlayer } from './player.js'

let socket = null
let reconnectTimer = null
let heartbeatTimer = null
let refreshTimer = null
let currentId = null
let callbacks = null
let reconnectDelay = 1000
let generation = 0

export function attachZone(id, handlers) {
  if (currentId && currentId !== id) leaveCurrentZone()
  detachTimers()
  currentId = id
  callbacks = handlers
  const version = ++generation
  connect(version)
  heartbeatTimer = setInterval(async () => {
    try { await heartbeat(id, playback.itemId, playback.playing) }
    catch (e) { if ([1002, 1003, 3004].includes(e.code)) handlers.ended(e.message) }
  }, 20000)
  // WS 断线兜底及时钟校准，不依赖另一位成员触发推送。
  refreshTimer = setInterval(() => handlers.changed(), 10000)
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
      if (event.type === 'ENDED' || event.type === 'ERROR') callbacks.ended('域已结束或需要重新进入')
      else if (event.type === 'GLOW') callbacks.glow(event.itemId)
      else callbacks.changed()
    } catch { /* 非 JSON 或旧协议消息等待下一次权威快照 */ }
  })
  socket.onClose(() => {
    if (version !== generation || !currentId) return
    reconnectTimer = setTimeout(() => connect(version), reconnectDelay)
    reconnectDelay = Math.min(30000, reconnectDelay * 2)
  })
  socket.onError(() => { /* close 回调负责重连，定时快照仍可用 */ })
}
function detachTimers() {
  clearInterval(heartbeatTimer); clearInterval(refreshTimer); clearTimeout(reconnectTimer)
  const old = socket; socket = null
  generation++
  if (old) old.close({})
}
export function leaveCurrentZone() {
  const id = currentId
  currentId = null
  detachTimers()
  resetPlayer()
  if (id) return leaveZone(id).catch(() => {}) // 断网由服务端心跳宽限收口
  return Promise.resolve()
}
export function activeZoneId() { return currentId }
