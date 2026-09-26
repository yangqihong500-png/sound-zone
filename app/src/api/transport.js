import { API_BASE } from './constants.js'
const token = () => uni.getStorageSync('soundzone-token') || ''

/** 查询参数显式拼接；POST/PUT 请求体与 DELETE 查询不混用。 */
export async function rawRequest(method, path, data = {}, options = {}) {
  const query = options.query || ((method === 'GET' || method === 'DELETE') ? data : {})
  const suffix = Object.entries(query).filter(([, value]) => value !== undefined && value !== null)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&')
  return new Promise((resolve, reject) => {
    uni.request({
      url: API_BASE + path + (suffix ? `${path.includes('?') ? '&' : '?'}${suffix}` : ''),
      method,
      data: method === 'GET' || method === 'DELETE' ? undefined : data,
      header: { 'Content-Type': 'application/json', ...(token() ? { Authorization: `Bearer ${token()}` } : {}) },
      timeout: 15000,
      success: ({ data: body }) => {
        if (body?.code === 0) resolve(body.data)
        else reject(apiError(body))
      },
      fail: () => reject(new Error('连接失败，请检查网络后重试')),
    })
  })
}
export function apiError(body) {
  const error = new Error(body?.msg || '请求失败')
  error.code = body?.code
  error.data = body?.data
  return error
}
