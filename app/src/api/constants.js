/**
 * api/constants.js —— 联调常量
 */

// Demo 当前用户 ID（对应后端种子数据 DataInitializer 中 octave 用户，自增主键 = 4）
// 【待确认】正式版应从登录态解析，Demo 阶段硬编码
export const CURRENT_USER_ID = 4

// Demo 当前用户昵称（后端种子数据 octave 用户，用于判断"是否本人"）
export const CURRENT_USER_NAME = 'octave'

// 后端 baseURL（与 request.js 保持一致，仅作展示用）
export const API_BASE = 'http://localhost:8080/api'
