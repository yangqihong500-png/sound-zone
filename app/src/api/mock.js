/**
 * api/mock.js —— Demo 阶段数据层（v2：2026-09-24 第二次会议）
 *
 * 当前为本地模拟数据，接口形态与后端（Spring Boot，server/）约定一致：
 * 联调时仅需把各函数体替换为 uni.request('/api' + 对应路径)，页面与组件无需改动。
 *
 * v2 模型要点：FIFO 队列 / 10 分钟上传冷却 / 标签五类+过滤双模式 /
 * 公开私密域 / 图片绑定上传歌曲+撤回+半小时流 / 关注 / 无歌品值
 */

const MIN = 60 * 1000

/** 场景分类（首页上滑标签栏 / 发现页），决议 D9 */
export const SCENES = ['全部', '音乐', '学习', '健身', '旅行', '日系', '电子', '城市', '深夜']

/** 标签目录（五类，决议 D5），创建域页分组展示 */
export const TAG_CATALOG = {
  语言: ['华语', '粤语', '日语', '韩语', '英语', '纯音乐'],
  年代: ['70s', '80s', '90s', '00s', '10s', '20s'],
  风格: ['流行', '摇滚', '电子', '说唱', '民谣', '爵士', '古典', 'City Pop', 'Lo-Fi', '抖音热曲'],
  场景: ['自习', '健身', '旅行', '通勤', '睡前', '工作', '手工'],
  情绪: ['舒缓', '治愈', '亢奋', '忧郁', '情歌', '专注'],
}

/** 曲库（上传歌曲弹窗搜索源） */
const TRACKS = [
  { id: 1, title: 'Lemon', artist: '米津玄師', coverColor: '#a8b8c8', tags: ['日语', '10s', '流行', '舒缓'] },
  { id: 2, title: 'キセキ', artist: 'GReeeeN', coverColor: '#b5c4d4', tags: ['日语', '00s', '流行', '治愈'] },
  { id: 3, title: '夜に駆ける', artist: 'YOASOBI', coverColor: '#c3b8d9', tags: ['日语', '20s', '流行', '抖音热曲'] },
  { id: 4, title: 'River Flows in You', artist: 'Yiruma', coverColor: '#c9d4c5', tags: ['纯音乐', '00s', '古典', '舒缓', '自习'] },
  { id: 5, title: 'Midnight City', artist: 'M83', coverColor: '#d9c3b8', tags: ['英语', '10s', '电子', '亢奋', '健身'] },
  { id: 6, title: 'Blinding Lights', artist: 'The Weeknd', coverColor: '#e3c9d4', tags: ['英语', '20s', '电子', '流行', '健身'] },
  { id: 7, title: 'Plastic Love', artist: '竹内まりや', coverColor: '#b8cdd9', tags: ['日语', '80s', 'City Pop', '旅行'] },
  { id: 8, title: '真夜中のドア', artist: '松原みき', coverColor: '#d9cfb8', tags: ['日语', '70s', 'City Pop', '深夜'] },
  { id: 9, title: 'Clair de Lune', artist: 'Debussy', coverColor: '#e3c9cd', tags: ['纯音乐', '古典', '舒缓', '睡前'] },
  { id: 10, title: 'Lose Yourself', artist: 'Eminem', coverColor: '#a9c4b5', tags: ['英语', '00s', '说唱', '亢奋', '健身'] },
]

/** 当前用户（Demo 固定，无歌品值——2026-09-24 去游戏化） */
const USER = {
  id: 'me',
  name: 'octave',
  avatarColor: '#8c9bab',
  stats: { uploads: 12, likes: 47, moments: 6, following: 3 },
}

/** 域数据（stateful：上传/图片/冷却会真实改变状态，用于演示新机制） */
const ZONES = [
  {
    id: 1,
    name: '考研自习室',
    scene: '学习',
    visibility: 'PUBLIC',
    listeners: 87,
    host: '白桃乌龙',
    coverColor: '#a8b8c8',
    tags: ['舒缓', '专注'],
    filterMode: 'BAN',
    filterTags: ['抖音热曲', '亢奋'],
    nowPlaying: { trackId: 1, title: 'Lemon', artist: '米津玄師', by: 'octave', progress: 62 },
    queue: [
      { itemId: 101, title: 'キセキ', artist: 'GReeeeN', likes: 24, by: '白桃乌龙' },
      { itemId: 102, title: '夜に駆ける', artist: 'YOASOBI', likes: 19, by: 'octave' },
      { itemId: 103, title: 'River Flows in You', artist: 'Yiruma', likes: 15, by: '白桃乌龙' },
    ],
    moments: [
      { id: 1, userId: 'me', user: 'octave', color: '#b5c4d4', track: 'Lemon', time: Date.now() - 12 * MIN },
      { id: 2, userId: 'u2', user: '白桃乌龙', color: '#c9d4c5', text: '今天也是满座', track: 'キセキ', time: Date.now() - 5 * MIN },
      { id: 3, userId: 'u3', user: '阿澈', color: '#d9cfb8', text: '咖啡续命中', track: 'Lemon', time: Date.now() - 22 * MIN },
    ],
    lastUploadAt: { me: Date.now() - 6 * MIN }, // 演示冷却：还剩约 4 分钟
  },
  {
    id: 2,
    name: '夜跑俱乐部',
    scene: '健身',
    visibility: 'PUBLIC',
    listeners: 45,
    host: '配速430',
    coverColor: '#a9c4b5',
    tags: ['电子', '亢奋'],
    filterMode: 'ALLOW',
    filterTags: ['电子', '亢奋', '健身'],
    nowPlaying: { trackId: 5, title: 'Midnight City', artist: 'M83', by: '配速430', progress: 35 },
    queue: [{ itemId: 201, title: 'Blinding Lights', artist: 'The Weeknd', likes: 21, by: '配速430' }],
    moments: [
      { id: 4, userId: 'u4', user: '配速430', color: '#d9c3b8', text: '珠江边 5km 打卡', track: 'Midnight City', time: Date.now() - 8 * MIN },
    ],
    lastUploadAt: {},
  },
  {
    id: 3,
    name: '京都深夜',
    scene: '旅行',
    visibility: 'PRIVATE',
    listeners: 12,
    host: '京都慢一点',
    coverColor: '#b8cdd9',
    tags: ['City Pop', '日语'],
    filterMode: 'BAN',
    filterTags: ['抖音热曲'],
    nowPlaying: { trackId: 7, title: 'Plastic Love', artist: '竹内まりや', by: '京都慢一点', progress: 48 },
    queue: [{ itemId: 301, title: '真夜中のドア', artist: '松原みき', likes: 28, by: '京都慢一点' }],
    moments: [
      { id: 5, userId: 'u5', user: '京都慢一点', color: '#b8cdd9', text: '鸭川的黄昏', track: 'Plastic Love', time: Date.now() - 3 * MIN },
    ],
    lastUploadAt: {},
  },
]

// ---------- 查询 ----------

export function getActiveZones() {
  // 只推公开域（决议 D2：私密域不参与分发）
  return Promise.resolve(ZONES.filter((z) => z.visibility === 'PUBLIC'))
}

export function getZonesByScene(scene) {
  const publicZones = ZONES.filter((z) => z.visibility === 'PUBLIC')
  if (!scene || scene === '全部') return Promise.resolve(publicZones)
  return Promise.resolve(publicZones.filter((z) => z.scene === scene))
}

export function getZoneDetail(id) {
  const zone = ZONES.find((z) => z.id === Number(id))
  return Promise.resolve(zone ? JSON.parse(JSON.stringify(zone)) : null)
}

export function getCurrentUser() {
  return Promise.resolve(USER)
}

// ---------- 创建域（创建域页） ----------

export function createZone(req) {
  // req: { name, scene, tags, filterMode, filterTags, visibility, password }
  const zone = {
    id: Date.now(),
    name: req.name,
    scene: req.scene,
    visibility: req.visibility || 'PUBLIC',
    listeners: 1,
    host: USER.name,
    coverColor: req.coverColor || '#a8b8c8',
    tags: req.tags || [],
    filterMode: req.filterMode || 'BAN',
    filterTags: req.filterTags || [],
    nowPlaying: null,
    queue: [],
    moments: [],
    lastUploadAt: {},
  }
  ZONES.push(zone)
  return Promise.resolve(zone)
}

// ---------- 上传歌曲（冷却 + 过滤 + FIFO） ----------

/** 冷却剩余秒数（决议 D4：单用户单域 10 分钟 1 首） */
export function getCooldown(zoneId) {
  const zone = ZONES.find((z) => z.id === Number(zoneId))
  const last = zone?.lastUploadAt?.me
  if (!last) return Promise.resolve(0)
  const remain = 10 * MIN - (Date.now() - last)
  return Promise.resolve(Math.max(0, Math.ceil(remain / 1000)))
}

/** 曲库搜索（上传歌曲弹窗） */
export function searchTracks(keyword) {
  if (!keyword) return Promise.resolve(TRACKS)
  const kw = keyword.toLowerCase()
  return Promise.resolve(
    TRACKS.filter((t) => t.title.toLowerCase().includes(kw) || t.artist.toLowerCase().includes(kw))
  )
}

/**
 * 上传歌曲：① 冷却校验 → ② 域级过滤（BAN/ALLOW）→ ③ 入队尾 FIFO
 * 返回 { code, message, queueItem }
 */
export function uploadSong(zoneId, trackId) {
  const zone = ZONES.find((z) => z.id === Number(zoneId))
  const track = TRACKS.find((t) => t.id === Number(trackId))
  if (!zone || !track) return Promise.resolve({ code: 2001, message: '域或歌曲不存在' })

  // ① 冷却（决议 D4）
  const last = zone.lastUploadAt.me
  if (last && Date.now() - last < 10 * MIN) {
    const remain = Math.ceil((10 * MIN - (Date.now() - last)) / 1000)
    return Promise.resolve({ code: 3005, message: `冷却剩余 ${remain} 秒`, remainSeconds: remain })
  }

  // ② 过滤双模式（决议 D5）
  const hit = track.tags.some((t) => zone.filterTags.includes(t))
  if (zone.filterMode === 'BAN' && hit) {
    return Promise.resolve({ code: 3002, message: `「${track.title}」的标签被本域禁止` })
  }
  if (zone.filterMode === 'ALLOW' && !hit) {
    return Promise.resolve({ code: 3009, message: `「${track.title}」不在本域允许范围内` })
  }

  // ③ 入队尾（FIFO）
  const item = { itemId: Date.now(), title: track.title, artist: track.artist, likes: 0, by: USER.name }
  zone.queue.push(item)
  zone.lastUploadAt.me = Date.now()
  return Promise.resolve({ code: 0, message: '已加入歌单', queueItem: item })
}

// ---------- 图片分享（绑定歌曲 + 半小时流 + 撤回） ----------

/** 发布图片分享：绑定当前播放或指定歌曲（决议 D6） */
export function uploadImage(zoneId, { imageUrl, color, trackTitle, text }) {
  const zone = ZONES.find((z) => z.id === Number(zoneId))
  if (!zone) return Promise.resolve({ code: 2001, message: '域不存在' })
  const moment = {
    id: Date.now(),
    userId: USER.id,
    user: USER.name,
    imageUrl: imageUrl || null,
    color: color || '#b5c4d4',
    text: text || null,
    track: trackTitle || zone.nowPlaying?.title || null,
    time: Date.now(),
  }
  zone.moments.unshift(moment)
  return Promise.resolve({ code: 0, moment })
}

/** 动态详情页：半小时内图片流（时间倒序） */
export function getMomentFeed(zoneId) {
  const zone = ZONES.find((z) => z.id === Number(zoneId))
  if (!zone) return Promise.resolve([])
  const cutoff = Date.now() - 30 * MIN
  return Promise.resolve(zone.moments.filter((m) => m.time >= cutoff))
}

/** 撤回自己的图片（仅本人，决议 D6） */
export function withdrawMoment(zoneId, momentId) {
  const zone = ZONES.find((z) => z.id === Number(zoneId))
  if (!zone) return Promise.resolve({ code: 2001, message: '域不存在' })
  zone.moments = zone.moments.filter((m) => !(m.id === momentId && m.userId === USER.id))
  return Promise.resolve({ code: 0 })
}

// ---------- 互动与关注 ----------

/** 收藏当前播放（触发上传者微光提示——Demo 用 Toast 模拟） */
export function collectTrack(zoneId, trackId) {
  console.log(`[mock] 域 ${zoneId} 收藏歌曲 ${trackId}，上传者将收到微光提示`)
  return Promise.resolve({ code: 0 })
}

const following = new Set(['白桃乌龙', '京都慢一点'])
export function isFollowing(name) {
  return Promise.resolve(following.has(name))
}
export function toggleFollow(name) {
  following.has(name) ? following.delete(name) : following.add(name)
  return Promise.resolve(following.has(name))
}
