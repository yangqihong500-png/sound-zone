package com.soundzone.moment.controller;

import com.soundzone.auth.service.CurrentUser;
import com.soundzone.common.Result;
import com.soundzone.moment.dto.MomentCreateRequest;
import com.soundzone.moment.service.MomentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class MomentController {
    private final com.soundzone.common.ResourceLocator locator;
    private final MomentService moments;
    private final CurrentUser current;

    @PostMapping(value = "/zones/{zoneId}/moments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> create(
            @PathVariable Long zoneId,
            @Valid @ModelAttribute MomentCreateRequest req,
            @RequestParam MultipartFile file) {
        return Result.ok(moments.create(zoneId, current.id(), req, file));
    }

    @GetMapping("/zones/{zoneId}/moments/feed")
    public Result<?> feed(@PathVariable Long zoneId) {
        return Result.ok(moments.feed(zoneId, current.id()));
    }

    @DeleteMapping("/moments/{id}")
    public Result<?> withdraw(@PathVariable Long id) {
        moments.withdraw(locator.momentZone(id), id, current.id());
        return Result.ok();
    }

    @PutMapping("/moments/{id}/reaction")
    public Result<?> react(@PathVariable Long id, @RequestBody Reaction req) {
        return Result.ok(moments.react(locator.momentZone(id), id, current.id(), req.type()));
    }

    @GetMapping("/moments/{id}/image")
    public ResponseEntity<FileSystemResource> image(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(moments.image(id, current.id())));
    }

    public record Reaction(String type) {}
}
