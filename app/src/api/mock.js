/** 保留既有 import 路径；所有业务请求均进入真实后端。 */
import { get, post, put, del, apiError } from './request.js'
import { API_BASE } from './constants.js'
import { ensureSession, token } from './session.js'
import { QUICK_SCENE_VALUES } from '@/constants/scenes.js'

export const SCENES = ['全部', ...QUICK_SCENE_VALUES]
export const getTagCatalog = () => get('/tracks/tags', {}, { auth: false })
export const getActiveZones = (keyword = '') => get('/zones/active', { keyword }, { auth: false })
export const getZonesByScene = (scene, keyword = '') => get('/zones/active', { scene, keyword }, { auth: false })
export const getZoneDetail = (id) => get(`/zones/${id}`)
export const joinZone = (id, credentials = {}) => post(`/zones/${id}/join`, credentials)
export const leaveZone = (id) => post(`/zones/${id}/leave`)
export const heartbeat = (id, itemId, playing) => post(`/zones/${id}/heartbeat`, { itemId, playing })
export const createZone = (req) => post('/zones', req)
export async function createZoneWithCover(req, filePath) {
  await ensureSession()
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE}/zones/with-cover`,
      filePath,
      name: 'file',
      header: { Authorization: `Bearer ${token()}` },
      formData: { payload: JSON.stringify(req) },
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          body.code === 0 ? resolve(body.data) : reject(apiError(body))
        } catch { reject(new Error('域封面上传响应异常')) }
      },
      fail: () => reject(new Error('域封面上传失败，请重试')),
    })
  })
}
export const updateZone = (id, req) => put(`/zones/${id}`, req)
export const getInvite = (id) => get(`/zones/${id}/invite`)
export const reportZone = (id, reason) => post(`/zones/${id}/reports`, { reason })
export const uploadSong = (zoneId, trackId) => post(`/zones/${zoneId}/queue`, { trackId })
export const getCooldown = (zoneId) => get(`/zones/${zoneId}/cooldown`).then((d) => d.remainSeconds)
export const likeQueueItem = (itemId, active) => put(`/queue/${itemId}/like`, { active })
export const searchTracks = (keyword) => get('/tracks/search', { keyword: keyword || '' })
export const getPlaybackSource = (zoneId, trackId) => get(`/tracks/${trackId}/playback`, { zoneId })
export const getMomentFeed = (zoneId) => get(`/zones/${zoneId}/moments/feed`)
export const getMoment = (id) => get(`/moments/${id}`)
export const withdrawMoment = (zoneId, momentId) => del(`/moments/${momentId}`)
export const reactMoment = (id, type) => put(`/moments/${id}/reaction`, { type })
export const collectTrack = (zoneId, itemId, active) => put(`/zones/${zoneId}/collection`, { itemId, active })
export const removeCollection = (trackId) => del(`/users/me/collections/${trackId}`)
export const getCurrentUser = () => get('/sessions/me')
export async function uploadProfileCover(filePath) {
  await ensureSession()
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE}/users/me/cover`, filePath, name: 'file',
      header: { Authorization: `Bearer ${token()}` },
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          body.code === 0 ? resolve(body.data) : reject(apiError(body))
        } catch { reject(new Error('背景上传响应异常')) }
      },
      fail: () => reject(new Error('背景上传失败，请重试')),
    })
  })
}
export const getListeningSummary = () => get('/users/me/listening-summary')
export const getUserProfile = (userId) => get(`/users/${userId}/profile`)
export const followUser = (id) => post(`/users/${id}/follow`)
export const unfollowUser = (id) => del(`/users/${id}/follow`)
export const getMyList = (kind) => get(`/users/me/${kind}`)
export const getConversation = (userId) => get(`/messages/users/${userId}`)
export const sendDirectMessage = (userId, body) => post(`/messages/users/${userId}`, { body })
export const getNotifications = () => get('/notifications')
export const getUnreadNotificationCount = () => get('/notifications/unread-count').then((data) => data.count || 0)
export const markNotificationRead = (id) => put(`/notifications/${id}/read`)
export const markAllNotificationsRead = () => put('/notifications/read-all')
export const sendZoneInvite = (zoneId, recipientId) => post(`/zones/${zoneId}/invites`, { recipientId })
export const resolveMediaUrl = (path) => path?.startsWith('/') ? API_BASE + path : (path || '')

export async function uploadImage(zoneId, { filePath, queueItemId, trainingConsent }) {
  await ensureSession()
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE}/zones/${zoneId}/moments`, filePath, name: 'file',
      header: { Authorization: `Bearer ${token()}` },
      formData: { queueItemId: String(queueItemId), trainingConsent: String(trainingConsent) },
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          body.code === 0 ? resolve(body.data) : reject(apiError(body))
        } catch { reject(new Error('图片上传响应异常')) }
      },
      fail: () => reject(new Error('图片上传失败，请重试')),
    })
  })
}
/** 私密图片带身份下载，不开放静态 URL；服务端撤回后禁止再次读取。 */
export async function loadImage(path) {
  await ensureSession()
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url: API_BASE + path, header: { Authorization: `Bearer ${token()}` },
      success: (res) => res.statusCode === 200 ? resolve(res.tempFilePath) : reject(new Error('图片暂不可查看')),
      fail: () => reject(new Error('图片加载失败')),
    })
  })
}
