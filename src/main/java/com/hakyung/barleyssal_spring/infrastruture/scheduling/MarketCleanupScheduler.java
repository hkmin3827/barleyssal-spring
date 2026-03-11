package com.hakyung.barleyssal_spring.infrastruture.scheduling;

import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderRepository;
import com.hakyung.barleyssal_spring.domain.order.OrderStatus;
import com.hakyung.barleyssal_spring.global.exception.DataArchiveException;
import com.hakyung.barleyssal_spring.infrastruture.elastic.OrderHistoryDoc;
import com.hakyung.barleyssal_spring.infrastruture.elastic.OrderSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketCleanupScheduler {

    private final OrderRepository orderRepository;
    private final StringRedisTemplate redisTemplate;
    private final OrderSearchRepository orderSearchRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void maintenanceCycle() {
        log.info("Starting daily maintenance cleanup...");

        int expiredCount = orderRepository.bulkUpdateStatus(
                OrderStatus.EXPIRED, 
                Set.of(OrderStatus.PENDING, OrderStatus.SUBMITTED)
        );
        log.info("DB Cleanup: {} orders marked as EXPIRED", expiredCount);

        try {
            archiveAndDeleteOldOrders();
        } catch (Exception e) {
            log.error("Critical: Archive process failed but DB status update is kept.", e);
        }
        clearRedisKeys("orders:pending:*");
        clearRedisKeys("order:meta:*");
        
        log.info("Redis Cleanup: Pending orders and metadata cleared");
    }

    private void clearRedisKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void archiveAndDeleteOldOrders() {
        Instant threshold = Instant.now().minus(3, ChronoUnit.DAYS);
        int batchSize = 1000;

        try{
            while (true) {
                List<Order> orders = orderRepository.findTop1000ByCreatedAtBefore(threshold);

                if (orders.isEmpty()) break;

                List<OrderHistoryDoc> docs = orders.stream()
                        .map(OrderHistoryDoc::from)
                        .toList();
                orderSearchRepository.saveAll(docs);

                orderRepository.deleteAllInBatch(orders);

                log.info("Archived and deleted {} orders...", orders.size());

                if (orders.size() < batchSize) break;
            }
        } catch (Exception e) {
            throw new DataArchiveException(e.getMessage(), e.getCause());
        }
    }
}