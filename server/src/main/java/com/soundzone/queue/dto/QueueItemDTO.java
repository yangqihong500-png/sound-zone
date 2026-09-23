package com.soundzone.queue.dto;

/** 队列条目响应（rank 由服务层按排序位置生成） */
public record QueueItemDTO(
        Long itemId,
        Integer rank,
        String title,
        String artist,
        Integer likes,
        String by,
        String status       // QUEUED / PRESET / PLAYING
) {}
