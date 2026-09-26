/** 真机或正式部署请配置 VITE_API_BASE；本地 H5 以打开页面的主机名访问后端。 */
let defaultBase = 'http://localhost:8080/api'
// #ifdef H5
defaultBase = `${window.location.protocol}//${window.location.hostname}:8080/api`
// #endif
export const API_BASE = (import.meta.env.VITE_API_BASE || defaultBase).replace(/\/+$/, '')
