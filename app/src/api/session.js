import { reactive } from 'vue'
import { rawRequest } from './transport.js'
const get = (path, data, options) => rawRequest('GET', path, data, options)
const post = (path, data, options) => rawRequest('POST', path, data, options)

export const session = reactive({ userId: null, name: '', guest: false, passwordAuth: false })
let pending = null
let ready = false
export const token = () => uni.getStorageSync('soundzone-token') || ''
export function invalidateSession() {
  ready = false
  uni.removeStorageSync('soundzone-token')
  Object.assign(session, { userId: null, name: '', guest: false, passwordAuth: false })
}

/** 独立 App 默认分配游客身份；宿主登录只在关闭游客且已提供适配器时启用。 */
export function ensureSession() {
  if (ready) return Promise.resolve(session)
  if (pending) return pending
  pending = initialize().finally(() => { pending = null })
  return pending
}
async function initialize() {
  const config = await get('/sessions/config', {}, { auth: false })
  session.passwordAuth = Boolean(config.password)
  if (token()) {
    try {
      const current = await get('/sessions/current', {}, { auth: false })
      Object.assign(session, current)
      ready = true
      return session
    } catch (e) {
      if (e.code !== 1002) throw e
      uni.removeStorageSync('soundzone-token')
    }
  }
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

function accept(result) {
  uni.setStorageSync('soundzone-token', result.token)
  Object.assign(session, { userId: result.userId, name: result.name, guest: result.guest })
  ready = true
  return session
}

/** 注册会把当前游客账号原地升级，已创建的域、收藏和关注不会丢失。 */
export async function registerAccount(username, password) {
  return accept(await post('/sessions/register', { username, password }, { auth: false }))
}

export async function loginAccount(username, password) {
  return accept(await post('/sessions/login', { username, password }, { auth: false }))
}

export async function logoutAccount() {
  try { await post('/sessions/logout', {}, { auth: false }) }
  finally { invalidateSession() }
}
