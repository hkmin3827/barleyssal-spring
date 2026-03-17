package com.hakyung.barleyssal_spring.infrastruture.elastic;

import com.hakyung.barleyssal_spring.domain.user.UserRepository;
import com.hakyung.barleyssal_spring.global.utils.TimeConverter;
import com.hakyung.barleyssal_spring.infrastruture.kafka.events.ExecutionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeStatisticsService {
    private final TradeStatsRepository tradeStatsRepository; // 상위 추상화 (Repository)
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @jakarta.annotation.PostConstruct
    public void checkBean() {
        log.info("★★★★★ TradeStatisticsService 가 빈으로 등록되었습니다! ★★★★★");
    }

    @KafkaListener(
            topics = "execution.event",
            groupId = "barleyssal-stats-group"
    )
    public void processTradeStats(String message, Acknowledgment ack) {
        try {
            log.info("▶▶▶▶▶ [KAFKA 수신 성공] raw message: {}", message);

            ExecutionEvent event = objectMapper.readValue(message, ExecutionEvent.class);
            log.info("Indexing trade stats for ES: orderId={}", event.orderId());
            log.info("▶▶▶▶▶ [매핑 완료] orderId: {}", event.orderId());

            TradeStatsDoc stats = new TradeStatsDoc(
                    null,
                    Long.valueOf(event.userId()),
                    event.userName(),
                    event.stockCode(),
                    event.orderSide(),
                    event.executedPrice().doubleValue(),
                    Long.valueOf(event.executedQuantity()),
                    calculateProfitRate(event),
                    TimeConverter.toLocalDateTime(event.timestamp())
            );

            tradeStatsRepository.save(stats);
            log.info("▶▶▶▶▶ [ES 저장 완료]");

            ack.acknowledge();
            log.debug("TradeStatsDoc indexed: stockCode={} side={}", event.stockCode(), event.orderSide());
        } catch (Exception e) {
            log.error("▶▶▶▶▶ [에러] 데이터 처리 중 예외 발생", e);
        }
    }

    private Double calculateProfitRate(ExecutionEvent event) {
        try {
            if (!"SELL".equals(event.orderSide())) {
                return 0.0;
            }

            String metaKey = "account:holdings:meta:" + event.userId();
            Object metaJson = redisTemplate.opsForHash().get(metaKey, event.stockCode());

            if (metaJson != null) {
                Map<String, Object> meta = objectMapper.readValue(metaJson.toString(), Map.class);
                BigDecimal avgPrice = new BigDecimal(meta.get("avgPrice").toString());
                BigDecimal executedPrice = event.executedPrice();

                if (avgPrice.compareTo(BigDecimal.ZERO) > 0) {
                    return executedPrice.subtract(avgPrice)
                            .divide(avgPrice, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
                }
            }
        } catch (Exception e) {
            log.warn("수익률 계산 실패 : {}", e.getMessage());
        }
        return 0.0;
    }
}