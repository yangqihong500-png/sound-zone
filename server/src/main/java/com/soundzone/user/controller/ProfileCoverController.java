package com.soundzone.user.controller;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.moment.service.ImageStorage;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.time.Duration;

/** 用户主动上传的主页背景公开可读；与需要身份校验的动态图片严格隔离。 */
@RestController
@RequiredArgsConstructor
public class ProfileCoverController {
    private final ImageStorage storage;

    @GetMapping("/profile-covers/{key:.+}")
    public ResponseEntity<FileSystemResource> image(@PathVariable String key) {
        var path = storage.resolveProfileCover(key);
        if (!Files.isRegularFile(path)) throw new BizException(ResultCode.PROFILE_COVER_NOT_FOUND);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .contentType(MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }
}
