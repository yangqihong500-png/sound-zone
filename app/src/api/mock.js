/**
 * api/mock.js —— Demo 阶段数据层
 *
 * 当前全部为本地模拟数据，接口形态与后端（FastAPI，见 docs/05）约定一致：
 * 后端就绪后，仅需把各函数体替换为 uni.request / WebSocket 调用，
 * 页面与组件无需任何改动。
 *
 * 数据结构设计依据：docs/01（域三件套）、docs/02（队列/审美反馈）
 */

/** 场景分类（发现页筛选用），与"场景标签归一化"对应 */
export const SCENES = ['全部', '自习', '健身', '旅行', '手工', '工作', '深夜']

/** 模拟域列表 */
const ZONES = [
  {
    id: 1,
    name: '考研自习室',
    scene: '自习',
    listeners: 87,
    host: '白桃乌龙',
    coverColor: '#9FE1CB',
    tags: ['舒缓', '纯音乐'],
    bannedTags: ['抖音热曲', '情歌'],
    nowPlaying: { title: 'Lemon', artist: '米津玄師', by: '小鹿', progress: 62 },
    queue: [
      { rank: 1, title: 'キセキ', artist: 'GReeeeN', likes: 24, by: '阿澈' },
      { rank: 2, title: '夜に駆ける', artist: 'YOASOBI', likes: 19, by: 'Momo' },
      { rank: 3, title: 'River Flows in You', artist: 'Yiruma', likes: 15, by: '白桃乌龙' },
    ],
    moments: [
      { id: 1, text: '图书馆 19:20', track: 'Lemon', time: '19:20', color: '#B5D4F4' },
      { id: 2, text: '刷完这套题就睡', track: 'キセキ', time: '19:05', color: '#F5C4B3' },
      { id: 3, text: '今天也是满座', track: 'River Flows in You', time: '18:47', color: '#C0DD97' },
      { id: 4, text: '咖啡续命中', track: 'Lemon', time: '18:12', color: '#F4C0D1' },
    ],
  },
  {
    id: 2,
    name: '夜跑俱乐部',
    scene: '健身',
    listeners: 45,
    host: '配速430',
    coverColor: '#F0997B',
    tags: ['电子', '节奏'],
    bannedTags: ['舒缓'],
    nowPlaying: { title: 'Midnight City', artist: 'M83', by: '北巷', progress: 35 },
    queue: [
      { rank: 1, title: 'Blinding Lights', artist: 'The Weeknd', likes: 21, by: '配速430' },
      { rank: 2, title: 'Stronger', artist: 'Kanye West', likes: 17, by: '北巷' },
    ],
    moments: [
      { id: 1, text: '珠江边 5km 打卡', track: 'Midnight City', time: '20:15', color: '#F0997B' },
      { id: 2, text: '今晚风很舒服', track: 'Blinding Lights', time: '19:58', color: '#85B7EB' },
    ],
  },
  {
    id: 3,
    name: '日本 solo trip',
    scene: '旅行',
    listeners: 62,
    host: '京都慢一点',
    coverColor: '#85B7EB',
    tags: ['City Pop', '日系'],
    bannedTags: ['抖音热曲'],
    nowPlaying: { title: 'Plastic Love', artist: '竹内まりや', by: '京都慢一点', progress: 48 },
    queue: [
      { rank: 1, title: '真夜中のドア', artist: '松原みき', likes: 28, by: '旅人K' },
      { rank: 2, title: 'First Love', artist: '宇多田ヒカル', likes: 22, by: 'Sakura' },
    ],
    moments: [
      { id: 1, text: '鸭川的黄昏', track: 'Plastic Love', time: '18:40', color: '#85B7EB' },
      { id: 2, text: '便利店饭团晚餐', track: '真夜中のドア', time: '17:55', color: '#FAC775' },
    ],
  },
  {
    id: 4,
    name: '拼豆手作坊',
    scene: '手工',
    listeners: 23,
    host: '豆豆本豆',
    coverColor: '#F4C0D1',
    tags: ['轻音乐', '治愈'],
    bannedTags: ['重金属'],
    nowPlaying: { title: 'Clair de Lune', artist: 'Debussy', by: '豆豆本豆', progress: 71 },
    queue: [{ rank: 1, title: 'Gymnopédie No.1', artist: 'Erik Satie', likes: 12, by: '慢半拍' }],
    moments: [{ id: 1, text: '星之卡比完工！', track: 'Clair de Lune', time: '16:30', color: '#F4C0D1' }],
  },
  {
    id: 5,
    name: '深夜写代码',
    scene: '深夜',
    listeners: 39,
    host: 'NullPointer',
    coverColor: '#AFA9EC',
    tags: ['Lo-Fi', '轻电子'],
    bannedTags: ['情歌', '抖音热曲'],
    nowPlaying: { title: 'Snowfall', artist: 'Øneheart', by: 'Refactor', progress: 55 },
    queue: [
      { rank: 1, title: 'weightless', artist: 'Marconi Union', likes: 16, by: 'NullPointer' },
      { rank: 2, title: 'Midnight', artist: 'Jinsang', likes: 11, by: 'Refactor' },
    ],
    moments: [
      { id: 1, text: 'bug 终于修了', track: 'Snowfall', time: '23:40', color: '#AFA9EC' },
      { id: 2, text: '再写最后一个需求', track: 'Midnight', time: '23:02', color: '#B4B2A9' },
    ],
  },
  {
    id: 6,
    name: '健身铁馆',
    scene: '健身',
    listeners: 51,
    host: '卧推100',
    coverColor: '#97C459',
    tags: ['摇滚', '说唱'],
    bannedTags: ['舒缓', '纯音乐'],
    nowPlaying: { title: 'Eye of the Tiger', artist: 'Survivor', by: '大重量小李', progress: 40 },
    queue: [
      { rank: 1, title: 'Lose Yourself', artist: 'Eminem', likes: 26, by: '卧推100' },
      { rank: 2, title: 'Believer', artist: 'Imagine Dragons', likes: 18, by: '大重量小李' },
    ],
    moments: [{ id: 1, text: '新 PR！', track: 'Lose Yourself', time: '19:45', color: '#97C459' }],
  },
]

/** 当前登录用户（Demo 固定） */
const USER = {
  name: 'octave',
  avatarColor: '#31C27C',
  tasteScore: 86, // 歌品值 = 点歌被点赞率（docs/02 成长体系）
  stats: { requests: 32, likes: 214, moments: 18 },
}

/** 获取此刻活跃的域列表（首页/发现页） */
export function getActiveZones() {
  return Promise.resolve(ZONES)
}

/** 按场景筛选域 */
export function getZonesByScene(scene) {
  if (!scene || scene === '全部') return Promise.resolve(ZONES)
  return Promise.resolve(ZONES.filter((z) => z.scene === scene))
}

/** 获取域详情（含播放中/队列/碎片墙） */
export function getZoneDetail(id) {
  const zone = ZONES.find((z) => z.id === Number(id))
  return Promise.resolve(zone || null)
}

/** 获取当前用户信息（我的页） */
export function getCurrentUser() {
  return Promise.resolve(USER)
}

/**
 * 点歌（Demo：仅模拟成功返回）
 * 真实实现：POST /zones/{id}/queue → 服务端黑名单/白名单校验 → WS 广播
 */
export function requestSong(zoneId, keyword) {
  console.log(`[mock] 域 ${zoneId} 收到点歌：${keyword}`)
  return Promise.resolve({ code: 0, message: '已加入队列' })
}
