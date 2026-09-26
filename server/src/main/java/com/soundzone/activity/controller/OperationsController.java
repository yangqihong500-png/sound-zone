package com.soundzone.activity.controller;

import com.soundzone.activity.service.MetricsService;
import com.soundzone.auth.service.AdminAccess;
import com.soundzone.common.*;
import com.soundzone.moment.service.*;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class OperationsController {
    private final AdminAccess admin;
    private final TrainingExportService training;
    private final MetricsService metrics;

    @GetMapping("/metrics")
    public Result<?> metrics(
            @RequestParam LocalDate date,
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        admin.check(key);
        return Result.ok(metrics.daily(date));
    }

    @PostMapping("/training/exports")
    public Result<?> export(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        admin.check(key);
        return Result.ok(training.export());
    }

    @GetMapping("/training/exports/{batch}/revocations")
    public Result<?> revoked(
            @PathVariable String batch,
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        admin.check(key);
        return Result.ok(training.revocations(batch));
    }

    @GetMapping("/training/images/{id}")
    public ResponseEntity<?> trainingImage(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        admin.check(key);
        return image(training.image(id));
    }

    private ResponseEntity<FileSystemResource> image(java.nio.file.Path path) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }

}
