package com.soundzone.queue.entity;

/** 队列条目状态（docs/02 队列状态机） */
public enum QueueStatus {
    /** 正在播放（每个域同一时刻至多一条） */
    PLAYING,
    /** 活跃队列中，按得分排序等待播放 */
    QUEUED,
    /** 预存队列：不符合当前时段白名单，等允许时段自动转正（docs/02 决议 D3） */
    PRESET,
    /** 已播放 */
    PLAYED,
    /** 被域主移除 */
    REMOVED
}
