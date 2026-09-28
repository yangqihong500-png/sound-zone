import { API_BASE } from '@/api/constants.js'
import { token } from '@/api/session.js'

let socket = null
let reconnectTimer = null
let refreshTimer = null
let reconnectDelay = 1000
let generation = 0
let changed = null

/** 私信实时通知；断线时 5 秒轮询仍会拉取权威消息列表。 */
export function attachMessages(onChanged) {
  detachMessages()
  changed = onChanged
  const version = ++generation
  connect(version)
  refreshTimer = setInterval(() => changed?.(), 5000)
}

function connect(version) {
  if (version !== generation || !changed) return
  socket = uni.connectSocket({
    url: API_BASE.replace(/^http/, 'ws') + '/ws/messages',
    complete: () => {},
  })
  socket.onOpen(() => {
    if (version !== generation) return
    reconnectDelay = 1000
    socket.send({ data: JSON.stringify({ token: token() }) })
  })
  socket.onMessage(({ data }) => {
    if (version !== generation) return
    try {
      const event = JSON.parse(data)
      if (event.type === 'MESSAGE') changed?.()
    } catch { /* 轮询会继续同步 */ }
  })
  socket.onClose(() => {
    if (version !== generation || !changed) return
    reconnectTimer = setTimeout(() => connect(version), reconnectDelay)
    reconnectDelay = Math.min(30000, reconnectDelay * 2)
  })
  socket.onError(() => { /* close 回调负责重连 */ })
}

export function detachMessages() {
  clearTimeout(reconnectTimer)
  clearInterval(refreshTimer)
  const old = socket
  socket = null
  changed = null
  generation++
  if (old) old.close({})
}
