/**
 * api/request.js —— 统一请求层
 *
 * 封装 uni.request，统一 baseURL（后端 Spring Boot，端口 8080，前缀 /api）、
 * 统一响应解包（后端返回 { code, msg, data }，code=0 为成功）。
 *
 * 联调说明：
 * - baseURL 指向 VSCode 里跑的后端
 * - 后端 CorsConfig 已放行 http://localhost:* 与 http://127.0.0.1:*
 * - 切换环境时只需改 BASE_URL
 */
const BASE_URL = 'http://localhost:8080/api'

/**
 * 通用请求
 * @param {string} method GET/POST/DELETE
 * @param {string} path  接口路径（不含 /api 前缀），如 '/zones/active'
 * @param {object} data  请求体（POST）或查询参数（GET/DELETE）
 * @returns {Promise<any>} 解包后的 data 字段；失败时 reject 带 message
 */
export function request(method, path, data = {}) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + path,
      method,
      data,
      header: { 'Content-Type': 'application/json' },
      success: (res) => {
        // 后端统一响应 { code, msg, data }，code=0 成功
        const body = res.data || {}
        if (body.code === 0) {
          resolve(body.data)
        } else {
          const err = new Error(body.msg || '请求失败')
          err.code = body.code
          reject(err)
        }
      },
      fail: (err) => {
        reject(new Error('网络错误，请确认后端已启动（http://localhost:8080）'))
      },
    })
  })
}

export const get = (path, data) => request('GET', path, data)
export const post = (path, data) => request('POST', path, data)
export const del = (path, data) => request('DELETE', path, data)

export { BASE_URL }
