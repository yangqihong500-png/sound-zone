package com.soundzone.moment.entity;

/** 历史状态字段：新图片默认可展示，旧 PENDING 保留原值，旧 REJECTED 保持隐藏。 */
public enum ModerationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
