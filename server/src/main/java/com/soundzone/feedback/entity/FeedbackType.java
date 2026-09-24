package com.soundzone.feedback.entity;

/** 反馈事件类型（2026-09-24 决议 D7：收藏/点赞 + emoji 轻互动） */
public enum FeedbackType {
    /** 收藏（触发上传者微光提示） */
    COLLECT,
    /** 点赞 */
    LIKE,
    /** emoji 轻互动：爱心 */
    EMOJI_HEART,
    /** emoji 轻互动：大笑 */
    EMOJI_LAUGH
}
