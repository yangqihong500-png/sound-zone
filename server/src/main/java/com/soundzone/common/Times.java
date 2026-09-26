package com.soundzone.common;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class Times {
    private Times() {}

    public static long millis(LocalDateTime value) {
        return value.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }
}
