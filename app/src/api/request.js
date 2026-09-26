import { ensureSession, invalidateSession } from './session.js'
import { rawRequest } from './transport.js'
export { apiError } from './transport.js'

export async function request(method, path, data = {}, options = {}) {
  if (options.auth !== false) await ensureSession()
  try { return await rawRequest(method, path, data, options) }
  catch (e) {
    // 只重建后续登录态，不自动重试上传等写操作。
    if (e.code === 1002) invalidateSession()
    throw e
  }
}
export const get = (path, data, options) => request('GET', path, data, options)
export const post = (path, data, options) => request('POST', path, data, options)
export const put = (path, data, options) => request('PUT', path, data, options)
export const del = (path, data, options) => request('DELETE', path, data, options)
