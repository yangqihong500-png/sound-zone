package com.soundzone.zone.entity;

/** 标签过滤模式（2026-09-24 决议 D5） */
public enum FilterMode {
    /** 黑名单：禁止含这些标签的音乐 */
    BAN,
    /** 白名单：仅允许含这些标签的音乐 */
    ALLOW
}
