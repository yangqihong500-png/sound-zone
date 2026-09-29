package com.soundzone.zone.controller;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.moment.service.ImageStorage;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.time.Duration;

/** 域封面是公开展示素材；随机文件名避免暴露同目录中的私密动态图片。 */
@RestController
@RequiredArgsConstructor
public class ZoneCoverController {
    private final ImageStorage storage;

    @GetMapping("/zone-covers/{key:.+}")
    public ResponseEntity<FileSystemResource> image(@PathVariable String key) {
        var path = storage.resolveZoneCover(key);
        if (!Files.isRegularFile(path)) throw new BizException(ResultCode.ZONE_COVER_NOT_FOUND);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .contentType(MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }
}
