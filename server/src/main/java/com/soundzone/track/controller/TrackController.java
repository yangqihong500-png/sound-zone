package com.soundzone.track.controller;

import com.soundzone.auth.service.CurrentUser;
import com.soundzone.common.Result;
import com.soundzone.track.service.*;
import com.soundzone.zone.service.ZoneAccess;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tracks")
@RequiredArgsConstructor
public class TrackController {
    private final TrackService tracks;
    private final MusicProvider music;
    private final ZoneAccess access;
    private final CurrentUser current;

    @GetMapping("/search")
    public Result<?> search(@RequestParam(defaultValue = "") String keyword) {
        current.id(); // 搜索会把外部曲目映射到本地，必须有独立会话，避免匿名滥用写入。
        return Result.ok(tracks.search(keyword));
    }

    @GetMapping("/tags")
    public Result<?> tags() {
        return Result.ok(TagCatalog.CATALOG);
    }

    @GetMapping("/{id}/playback")
    public Result<?> playback(@PathVariable Long id, @RequestParam Long zoneId) {
        access.member(zoneId, current.id());
        return Result.ok(music.resolve(tracks.getById(id)));
    }
}
