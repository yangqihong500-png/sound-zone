package com.soundzone.zone.entity;

/** 番茄钟时段类型（docs/02 决议 D3） */
public enum PeriodType {
    /** 专注段（如 40 分钟，通常仅允许舒缓类标签） */
    FOCUS,
    /** 休息段（如 10–20 分钟，可放宽到流行等标签） */
    BREAK
}
