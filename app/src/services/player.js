import { reactive } from 'vue'
import { getPlaybackSource } from '@/api/mock.js'
import { API_BASE } from '@/api/constants.js'

export const playback = reactive({ playing: false, loading: false, message: '', needsGesture: false, itemId: null })
let audio = null
let generation = 0
let current = null
let offset = 0
let host = null
let lastSeek = 0
let currentStreamUrl = ''
let loadTimer = null
let endedHandler = null
let activityHandler = null

/** 由域详情注册；后台音频自然结束后立即读取服务端权威快照并衔接下一首。 */
export function setPlaybackEndedHandler(handler) {
  endedHandler = typeof handler === 'function' ? handler : null
}

/** 后台音频进度事件用于在系统节流普通定时器时继续维持域心跳。 */
export function setPlaybackActivityHandler(handler) {
  activityHandler = typeof handler === 'function' ? handler : null
}

export function resetPlayer() {
  generation++
  current = null
  currentStreamUrl = ''
  clearTimeout(loadTimer)
  if (audio) {
    detachAudioEvents(audio)
    // #ifdef H5
    audio.destroy()
    // #endif
    // #ifndef H5
    audio.stop()
    // #endif
    audio = null
  }
  if (host?.stop) host.stop()
  host = null
  Object.assign(playback, { playing: false, loading: false, itemId: null, message: '', needsGesture: false })
}
export function positionSeconds() {
  return current ? Math.max(0, Math.min(current.durationSec, (Date.now() + offset - current.startedAt) / 1000)) : 0
}
/** 服务端时钟补偿以请求中点估计；只有服务端快照可以触发换曲。 */
export async function syncPlayer(zone, requestStarted) {
  const now = zone.nowPlaying
  offset = zone.serverTime - ((requestStarted + Date.now()) / 2)
  if (!now) { resetPlayer(); return }
  if (playback.itemId === now.itemId) {
    current = now
    if (audio && playback.playing && Math.abs(audio.currentTime - positionSeconds()) > 1.2 && Date.now() - lastSeek > 3000) {
      audio.seek(positionSeconds()); lastSeek = Date.now()
    }
    if (host?.seek) host.seek(positionSeconds())
    return
  }
  resetPlayer()
  current = now
  playback.itemId = now.itemId
  const version = generation
  try {
    const source = await getPlaybackSource(zone.id, now.trackId)
    if (version !== generation) return
    if (source.kind === 'UNAVAILABLE') { playback.message = source.message; return }
    if (source.kind === 'HOST') {
      host = globalThis.SoundZoneHost?.player
      if (!host?.play) { playback.message = '该曲目需要尚未接入的外部播放器'; return }
      await host.play({ source: source.source, externalId: source.externalId, position: positionSeconds() })
      if (version === generation) playback.playing = true
      return
    }
    currentStreamUrl = absoluteStreamUrl(source.streamUrl)
    prepareAudio(version, true)
  } catch (e) { if (version === generation) playback.message = e.message }
}
function absoluteStreamUrl(url) {
  if (/^https?:\/\//i.test(url || '')) return url
  return `${API_BASE}${String(url || '').startsWith('/') ? '' : '/'}${url || ''}`
}
function prepareAudio(version, tryAutoplay) {
  clearTimeout(loadTimer)
  if (audio) {
    detachAudioEvents(audio)
    // #ifdef H5
    audio.destroy()
    // #endif
    // #ifndef H5
    audio.stop()
    // #endif
  }
  // H5 没有后台音频 API，仍使用普通上下文；App 与小程序使用系统后台音频。
  // #ifdef H5
  audio = uni.createInnerAudioContext()
  // #endif
  // #ifndef H5
  audio = uni.getBackgroundAudioManager()
  // #endif
  let canPlayHandled = false
  Object.assign(playback, { playing: false, loading: true, needsGesture: true, message: '正在连接音源…' })
  audio.autoplay = false
  audio.onPlay(() => {
    if (version !== generation) return
    clearTimeout(loadTimer)
    Object.assign(playback, { playing: true, loading: false, needsGesture: false, message: '正在同步播放' })
  })
  audio.onPause(() => { if (version === generation) playback.playing = false })
  audio.onEnded(() => {
    if (version !== generation) return
    playback.playing = false
    playback.message = '正在衔接下一首…'
    endedHandler?.()
  })
  audio.onTimeUpdate?.(() => {
    if (version === generation && playback.playing) activityHandler?.()
  })
  audio.onError((error) => {
    if (version !== generation) return
    clearTimeout(loadTimer)
    Object.assign(playback, {
      playing: false,
      loading: false,
      needsGesture: true,
      message: audioErrorMessage(error),
    })
  })
  audio.onCanplay(() => {
    if (version !== generation || canPlayHandled) return
    canPlayHandled = true
    clearTimeout(loadTimer)
    playback.loading = false
    playback.message = tryAutoplay ? '音源已就绪，正在尝试播放…' : '音源已就绪，请点击开始同步播放'
    audio.seek(positionSeconds())
    if (tryAutoplay) audio.play()
  })
  // #ifndef H5
  audio.title = current?.title || 'SoundZone'
  audio.singer = current?.artist || 'SoundZone'
  audio.coverImgUrl = current?.coverUrl || ''
  audio.startTime = positionSeconds()
  // #endif
  updateSystemMediaMetadata()
  audio.src = currentStreamUrl
  loadTimer = setTimeout(() => {
    if (version !== generation || playback.playing) return
    Object.assign(playback, { loading: false, needsGesture: true, message: '音源连接较慢，请点击重试' })
  }, 25000)
}
function detachAudioEvents(context) {
  context.offPlay?.()
  context.offPause?.()
  context.offEnded?.()
  context.offError?.()
  context.offCanplay?.()
  context.offTimeUpdate?.()
}

function updateSystemMediaMetadata() {
  // #ifdef H5
  if (!('mediaSession' in navigator) || typeof MediaMetadata === 'undefined') return
  const artwork = current?.coverUrl ? [{ src: absoluteStreamUrl(current.coverUrl) }] : []
  navigator.mediaSession.metadata = new MediaMetadata({
    title: current?.title || 'SoundZone',
    artist: current?.artist || 'SoundZone',
    album: '同频 SoundZone',
    artwork,
  })
  // #endif
}
function audioErrorMessage(error) {
  const detail = error?.errMsg || error?.message || ''
  if (/autoplay|gesture|NotAllowed/i.test(detail)) return '浏览器需要你点击一次，才能开始同步播放'
  return detail ? `音源加载失败：${detail}` : '音源加载失败，请点击重试'
}
/** 浏览器首播需要用户手势；加载失败时会重建音频上下文并重新请求音源。 */
export function unlockAudio() {
  if (!currentStreamUrl) return
  if (!audio || (!playback.loading && !playback.playing && playback.message.includes('失败'))) {
    prepareAudio(generation, false)
  }
  audio.seek(positionSeconds())
  audio.play()
}
