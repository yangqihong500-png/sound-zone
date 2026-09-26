import { reactive } from 'vue'
import { rawRequest } from './transport.js'
const get = (path, data, options) => rawRequest('GET', path, data, options)
const post = (path, data, options) => rawRequest('POST', path, data, options)

export const session = reactive({ userId: null, name: '', guest: false })
let pending = null
let ready = false
export const token = () => uni.getStorageSync('soundzone-token') || ''
export function invalidateSession() {
  ready = false
  uni.removeStorageSync('soundzone-token')
  Object.assign(session, { userId: null, name: '', guest: false })
}

/** 独立 App 默认分配游客身份；宿主登录只在关闭游客且已提供适配器时启用。 */
export function ensureSession() {
  if (ready) return Promise.resolve(session)
  if (pending) return pending
  pending = initialize().finally(() => { pending = null })
  return pending
}
async function initialize() {
  if (token()) {
    try {
      const profile = await get('/sessions/me', {}, { auth: false })
      session.userId = profile.id
      session.name = profile.name
      ready = true
      return session
    } catch (e) {
      if (e.code !== 1002) throw e
      uni.removeStorageSync('soundzone-token')
    }
  }
  const config = await get('/sessions/config', {}, { auth: false })
  let result
  if (config.guest ?? config.demo) result = await post('/sessions/guest', {}, { auth: false })
  else if (config.host) {
    const bridge = globalThis.SoundZoneHost
    if (!bridge?.getLoginCode) throw new Error('当前版本需要宿主登录，请从已接入的入口打开')
    const code = await bridge.getLoginCode()
    result = await post('/sessions/host', { code }, { auth: false })
  } else throw new Error('当前未开放登录，请联系管理员开启游客入口')
  uni.setStorageSync('soundzone-token', result.token)
  Object.assign(session, { userId: result.userId, name: result.name, guest: result.guest ?? result.demo })
  ready = true
  return session
}
