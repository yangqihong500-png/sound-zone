export const SCENE_CATEGORIES = [
  { key: 'daily', label: 'Daily' },
  { key: 'social', label: 'Social' },
  { key: 'movement', label: 'Movement' },
  { key: 'mood', label: 'Mood & time' },
]

export const SCENE_CATALOG = [
  { value: '自习', label: 'Study', icon: '✎', category: 'daily' },
  { value: '工作', label: 'Work', icon: '⌘', category: 'daily' },
  { value: '阅读', label: 'Reading', icon: '▤', category: 'daily' },
  { value: '通勤', label: 'Commute', icon: '↗', category: 'daily' },
  { value: '烹饪', label: 'Cooking', icon: '♨', category: 'daily' },
  { value: '家务', label: 'Chores', icon: '⌂', category: 'daily' },
  { value: '旅行', label: 'Travel', icon: '✈', category: 'daily' },
  { value: '手工', label: 'Craft', icon: '◇', category: 'daily' },
  { value: '聚会', label: 'Party', icon: '✦', category: 'social' },
  { value: '晚餐', label: 'Dinner', icon: '♨', category: 'social' },
  { value: '约会', label: 'Date', icon: '♡', category: 'social' },
  { value: '好友', label: 'Friends', icon: '◎', category: 'social' },
  { value: '游戏', label: 'Gaming', icon: '⌁', category: 'social' },
  { value: '家庭', label: 'Family', icon: '⌂', category: 'social' },
  { value: '健身', label: 'Fitness', icon: '↯', category: 'movement' },
  { value: '跑步', label: 'Running', icon: '↠', category: 'movement' },
  { value: '瑜伽', label: 'Yoga', icon: '◯', category: 'movement' },
  { value: '徒步', label: 'Hiking', icon: '△', category: 'movement' },
  { value: '骑行', label: 'Cycling', icon: '∞', category: 'movement' },
  { value: '散步', label: 'Walking', icon: '· ·', category: 'movement' },
  { value: '深夜', label: 'Late Night', icon: '☾', category: 'mood' },
  { value: '清晨', label: 'Morning', icon: '☼', category: 'mood' },
  { value: '雨天', label: 'Rainy Day', icon: '☂', category: 'mood' },
  { value: '放松', label: 'Relax', icon: '〜', category: 'mood' },
  { value: '专注', label: 'Focus', icon: '⊙', category: 'mood' },
  { value: '睡眠', label: 'Sleep', icon: 'Z', category: 'mood' },
]

export const QUICK_SCENE_VALUES = ['自习', '工作', '健身', '旅行', '深夜', '聚会']

const LEGACY_SCENES = {
  音乐: { value: '音乐', label: 'Music', icon: '♫', category: 'legacy' },
  日系: { value: '日系', label: 'J-Pop', icon: '✿', category: 'legacy' },
  电子: { value: '电子', label: 'Electronic', icon: '⌁', category: 'legacy' },
}

const SCENE_MAP = Object.fromEntries(SCENE_CATALOG.map((scene) => [scene.value, scene]))

export function getSceneMeta(value) {
  return SCENE_MAP[value] || LEGACY_SCENES[value] || {
    value,
    label: value || 'Zone',
    icon: '◌',
    category: 'custom',
  }
}

export function sceneLabel(value) {
  return getSceneMeta(value).label
}

export function sceneIcon(value) {
  return getSceneMeta(value).icon
}
