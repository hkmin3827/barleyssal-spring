package com.hakyung.barleyssal_spring.infrastrutrue.kafka;

import com.stocksim.core.infrastructure.kafka.events.ExecutionEvent;
import com.stocksim.core.infrastructure.kafka.events.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mock 체결 엔진
 * order.request 를 소비 → 현재가와 비교 → execution.event 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockMatchingEngineConsumer {

    public static final String TOPIC_EXECUTION_EVENT = "execution.event";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate            redisTemplate;

    @KafkaListener(
        topics     = OrderEventProducer.TOPIC_ORDER_REQUEST,
        groupId    = "mock-matcher",
        concurrency = "1"
    )
    public void onOrderRequest(OrderPlacedEvent event) {
        log.info("MockMatcher received: orderId={} symbol={} side={}", event.orderId(), event.symbol(), event.side());
        try {
            BigDecimal execPrice = resolveExecutionPrice(event);
            if (execPrice == null) {
                log.debug("Order not matched yet (limit price condition not met): orderId={}", event.orderId());
                return;
            }
            var execEvent = new ExecutionEvent(
                event.orderId(), event.accountId(), event.symbol(),
                event.side(), event.quantity(), execPrice
            );
            kafkaTemplate.send(TOPIC_EXECUTION_EVENT, execEvent.orderId(), execEvent);
            log.info("ExecutionEvent published: orderId={} price={}", event.orderId(), execPrice);
        } catch (Exception e) {
            log.error("MockMatcher error: orderId={}", event.orderId(), e);
        }
    }

    /** 체결 가격 결정 로직 (Mock) */
    private BigDecimal resolveExecutionPrice(OrderPlacedEvent event) {
        String priceStr = redisTemplate.opsForValue().get("market:price:" + event.symbol());
        BigDecimal currentPrice = (priceStr != null)
            ? new BigDecimal(priceStr)
            : BigDecimal.valueOf(50_000L);   // 기본값 (Redis 미적재 시)

        return switch (event.orderType()) {
            case "MARKET" -> currentPrice;
            case "LIMIT"  -> {
                if (event.limitPrice() == null) yield currentPrice;
                boolean executable = "BUY".equals(event.side())
                    ? currentPrice.compareTo(event.limitPrice()) <= 0
                    : currentPrice.compareTo(event.limitPrice()) >= 0;
                yield executable ? currentPrice : null;
            }
            default -> currentPrice;
        };
    }
}
