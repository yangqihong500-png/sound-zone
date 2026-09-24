package com.soundzone.queue.dto;

/** 上传冷却状态（决议 D4）：前端据此渲染按钮置灰与倒计时 */
public record CooldownDTO(
        long remainSeconds,      // 剩余冷却秒数，0 = 可上传
        int cooldownMinutes      // 冷却总时长（分钟），便于前端展示规则
) {}
