package com.hakyung.barleyssal_spring.global.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class TimeConverter {

    private static final ZoneId UTC  = ZoneOffset.UTC;
    public static final ZoneId KST  = ZoneId.of("Asia/Seoul");

    private TimeConverter() {}

    public static LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return LocalDateTime.now(KST);
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), KST);
    }

    public static long nowMillis() {
        return Instant.now().toEpochMilli();
    }


    public static LocalDateTime now() {
        return LocalDateTime.now(KST);
    }

    public static Instant toInstant(Long epochMillis) {
        if (epochMillis == null) {
            return Instant.now();
        }
        return Instant.ofEpochMilli(epochMillis);
    }
}