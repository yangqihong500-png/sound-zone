package com.soundzone.zone.entity;

/** 域可见性（2026-09-24 决议 D2） */
public enum ZoneVisibility {
    /** 公开域：可被推荐/搜索/分类展示，自由进入 */
    PUBLIC,
    /** 私密域：凭邀请链接或密码进入，不参与分发 */
    PRIVATE
}
