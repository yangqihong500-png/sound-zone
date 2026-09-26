package com.soundzone.moment.dto;

import jakarta.validation.constraints.*;

/** 文件通过 multipart 上传，歌曲从具体队列条目解析，禁止客户端提交任意 URL。 */
public record MomentCreateRequest(
        @NotNull Long queueItemId, @Size(max = 500) String text, boolean trainingConsent) {}
