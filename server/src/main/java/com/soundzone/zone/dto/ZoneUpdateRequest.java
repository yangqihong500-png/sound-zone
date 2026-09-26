package com.soundzone.zone.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

/** 修改主题及展示标签，不追溯改变已经准入的队列。 */
public record ZoneUpdateRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 32) String scene,
        Set<String> tags,
        @Size(max = 16) String coverColor) {}
