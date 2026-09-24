package com.soundzone.moment.entity;

/** 图片分享状态（2026-09-24 决议 D6：撤回机制） */
public enum MomentStatus {
    NORMAL,
    /** 已被上传者撤回（列表查询排除） */
    WITHDRAWN
}
