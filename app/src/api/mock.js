/**
 * api/mock.js —— 数据层（已切换到真实后端）
 *
 * 本文件对外暴露与后端 18 个接口一一对应的函数，页面/组件无需改动。
 * 底层走 api/request.js（uni.request + baseURL + 统一解包）。
 *
 * 说明：文件名保留 mock.js 是为了不破坏现有组件的 import 路径；
 * 若要切回本地演示数据，只需把各函数体替换为原来的本地实现。
 */
import { get, post, del } from './request.js'
import { CURRENT_USER_ID } from './constants.js'

// ========== 场景与标签目录（建域页分组展示，静态数据） ==========
// 场景值与后端种子数据 ZoneService.normalizeScene 归一化结果对齐（自习/健身/旅行/工作/深夜…）
export const SCENES = ['全部', '音乐', '自习', '健身', '旅行', '日系', '电子', '工作', '深夜']

export const TAG_CATALOG = {
  语言: ['华语', '粤语', '日语', '韩语', '英语', '纯音乐'],
  年代: ['70s', '80s', '90s', '00s', '10s', '20s'],
  风格: ['流行', '摇滚', '电子', '说唱', '民谣', '爵士', '古典', 'City Pop', 'Lo-Fi', '抖音热曲'],
  场景: ['自习', '健身', '旅行', '通勤', '睡前', '工作', '手工'],
  情绪: ['舒缓', '治愈', '亢奋', '忧郁', '情歌', '专注'],
}

// ========== 域 ==========

/** 活跃公开域列表（首页/发现页）→ GET /zones/active */
export function getActiveZones() {
  return get('/zones/active')
}

/** 按场景筛选（scene 为空或「全部」则不筛选）→ GET /zones/active?scene= */
export function getZonesByScene(scene) {
  return get('/zones/active', { scene: scene === '全部' ? '' : scene })
}

/** 域详情（播放中 + FIFO 队列 + 动态区 1-2 张）→ GET /zones/{id} */
export function getZoneDetail(id) {
  return get(`/zones/${id}`)
}

/** 进入域（私密域需密码/邀请码）→ POST /zones/{id}/join */
export function joinZone(id, { userId, password, inviteCode }) {
  return post(`/zones/${id}/join`, { userId, password, inviteCode })
}

/** 退出域 → POST /zones/{id}/leave */
export function leaveZone(id, userId) {
  return post(`/zones/${id}/leave`, { userId })
}

/** 创建域 → POST /zones */
export function createZone(req) {
  return post('/zones', {
    name: req.name,
    scene: req.scene,
    hostId: req.hostId || CURRENT_USER_ID, // Demo 当前用户（后端种子数据 octave = id 4）
    trackIds: req.trackIds || [],
    visibility: req.visibility,
    password: req.password || null,
    filterMode: req.filterMode,
    filterTags: req.filterTags || [],
    tags: req.tags || [],
    coverColor: req.coverColor || null,
  })
}

// ========== 上传与队列 ==========

/** 上传歌曲 → POST /zones/{zoneId}/queue（冷却→过滤→FIFO） */
export function uploadSong(zoneId, trackId) {
  return post(`/zones/${zoneId}/queue`, { trackId, userId: CURRENT_USER_ID })
}

/** 上传冷却剩余秒数 → GET /zones/{zoneId}/cooldown */
export function getCooldown(zoneId) {
  return get(`/zones/${zoneId}/cooldown`, { userId: CURRENT_USER_ID }).then((d) => d.remainSeconds ?? 0)
}

/** 点赞队列条目 → POST /queue/{itemId}/like */
export function likeQueueItem(itemId, userId) {
  return post(`/queue/${itemId}/like`, { userId: userId || CURRENT_USER_ID })
}

/** 搜曲（上传歌曲弹窗）→ GET /tracks/search?keyword= */
export function searchTracks(keyword) {
  return get('/tracks/search', { keyword: keyword || '' })
}

// ========== 图片分享 ==========

/** 发布图片分享（绑定歌曲）→ POST /zones/{zoneId}/moments */
export function uploadImage(zoneId, { imageUrl, color, trackTitle, text }) {
  return post(`/zones/${zoneId}/moments`, {
    userId: CURRENT_USER_ID,
    text: text || null,
    imageUrl: imageUrl || null,
    color: color || null,
    // trackId 由后端回退当前播放；如需指定本人上传的歌曲，另传 trackId
  })
}

/** 动态详情页：半小时图片流 → GET /zones/{zoneId}/moments/feed */
export function getMomentFeed(zoneId) {
  return get(`/zones/${zoneId}/moments/feed`)
}

/** 撤回图片（仅本人）→ DELETE /moments/{momentId}?userId= */
export function withdrawMoment(zoneId, momentId) {
  return del(`/moments/${momentId}`, { userId: CURRENT_USER_ID })
}

// ========== 互动与关注 ==========

/** 收藏/点赞/emoji → POST /zones/{zoneId}/heart */
export function collectTrack(zoneId, trackId) {
  return post(`/zones/${zoneId}/heart`, { userId: CURRENT_USER_ID, trackId, type: 'COLLECT' })
}

export function heartTrack(zoneId, trackId, type = 'LIKE') {
  return post(`/zones/${zoneId}/heart`, { userId: CURRENT_USER_ID, trackId, type })
}

// ========== 用户 ==========

/** 我的页信息 → GET /users/{userId}/profile */
export function getCurrentUser() {
  return get(`/users/${CURRENT_USER_ID}/profile`)
}

/** 任意用户主页信息 → GET /users/{userId}/profile（用户不存在时 reject，message 来自后端 2003） */
export function getUserProfile(userId) {
  return get(`/users/${userId}/profile`)
}

/** 关注用户 → POST /users/{userId}/follow?fromUserId=（当前用户关注目标用户） */
export function followUser(userId) {
  return post(`/users/${userId}/follow`, { fromUserId: CURRENT_USER_ID })
}

/** 取关 → DELETE /users/{userId}/follow?fromUserId= */
export function unfollowUser(userId) {
  return del(`/users/${userId}/follow`, { fromUserId: CURRENT_USER_ID })
}
