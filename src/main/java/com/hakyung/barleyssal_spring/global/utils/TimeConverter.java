package com.hakyung.barleyssal_spring.global.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 타임스탬프 변환 유틸리티.
 *
 * <p>Go 서버는 {@code time.Now().UnixMilli()} 로 epoch milliseconds(long)를 전송한다.
 * Spring 도메인 모델과 Elasticsearch 문서는 모두 이 클래스를 통해 변환하여
 * 타입 불일치({@code Instant} ↔ {@code Long} ↔ {@code LocalDateTime})가
 * 발생하지 않도록 한다.</p>
 *
 * <ul>
 *   <li>Kafka 수신 필드 타입 : {@code Long} (epoch millis)</li>
 *   <li>Elasticsearch 저장 타입 : {@code LocalDateTime} (UTC)</li>
 * </ul>
 */
public final class TimeConverter {

    private static final ZoneId UTC  = ZoneOffset.UTC;
    /** Elasticsearch 저장용 — 한국 표준시 (UTC+9) */
    public static final ZoneId KST  = ZoneId.of("Asia/Seoul");

    private TimeConverter() {}

    /**
     * epoch milliseconds → {@link LocalDateTime} (KST, Asia/Seoul).
     * Elasticsearch {@code date_hour_minute_second_millis} 포맷으로 한국 시간 기준 저장.
     *
     * @param epochMillis Go {@code time.Now().UnixMilli()} 로 생성된 long 값
     * @return KST 기준 LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return LocalDateTime.now(KST);
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), KST);
    }

    /**
     * 현재 시각을 epoch milliseconds(long)로 반환.
     * Go {@code utils.NowMillis()} 와 동일한 값을 생성한다.
     */
    public static long nowMillis() {
        return Instant.now().toEpochMilli();
    }

    /**
     * 현재 시각을 {@link LocalDateTime}(KST)으로 반환.
     * Elasticsearch 저장 시 한국 시간 기준으로 기록한다.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(KST);
    }

    /**
     * epoch milliseconds → {@link Instant}.
     * 도메인 이벤트 내부 처리용.
     */
    public static Instant toInstant(Long epochMillis) {
        if (epochMillis == null) {
            return Instant.now();
        }
        return Instant.ofEpochMilli(epochMillis);
    }
}