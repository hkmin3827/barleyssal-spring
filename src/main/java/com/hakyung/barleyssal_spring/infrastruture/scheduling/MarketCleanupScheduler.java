package com.hakyung.barleyssal_spring.infrastruture.scheduling;

import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderRepository;
import com.hakyung.barleyssal_spring.domain.order.OrderStatus;
import com.hakyung.barleyssal_spring.global.exception.DataArchiveException;
import com.hakyung.barleyssal_spring.global.exception.DataCleanupException;
import com.hakyung.barleyssal_spring.infrastruture.elastic.OrderHistoryDoc;
import com.hakyung.barleyssal_spring.infrastruture.elastic.OrderSearchRepository;
import com.hakyung.barleyssal_spring.infrastruture.elastic.TradeStatsDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final ElasticsearchOperations esOps;
    private final OrderBulkExpireScheduler orderBulkExpireScheduler;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void maintenanceCycle() {
        log.info("Starting daily maintenance cleanup...");

        try {
            orderBulkExpireScheduler.expireRemaining();
        } catch (Exception e) {
            log.error("Critical: Midnight fallback expire failed. Some orders may remain SUBMITTED.", e);
        }

        try {
            syncYesterdayOrdersToElastic();
        } catch (Exception e) {
            log.error("Critical: Archive process failed. : {}", e.getMessage());
        }

        try {
            deleteOldOrders();
        } catch (Exception e) {
            log.error("Critical: DB Old orders deletion failed. : {}", e.getMessage());
        }

        clearRedisKeys("orders:pending:*");
        clearRedisKeys("order:meta:*");

        log.info("Redis Cleanup: Pending orders and metadata cleared");
    }

    @Transactional
    @Scheduled(cron = "0 30 0 * * *")
    public void cleanupOldElasticData() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(14);

        Criteria criteria = new Criteria("timestamp").lessThan(threshold);

        Query query = new CriteriaQuery(criteria);

        DeleteQuery deleteQuery = DeleteQuery.builder(query)
                .build();

        esOps.delete(deleteQuery, OrderHistoryDoc.class);
        esOps.delete(deleteQuery, TradeStatsDoc.class);
        log.info("Elasticsearch: 14일 경과된 통계 데이터 삭제 완료");
    }

    private void clearRedisKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void syncYesterdayOrdersToElastic() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime nowKst = ZonedDateTime.now(zoneId);
        Instant startOfYesterday = nowKst.minusDays(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        Instant startOfToday = nowKst.truncatedTo(ChronoUnit.DAYS).toInstant();

        long lastId = 0L;
        int batchSize = 1000;
        int totalSynced = 0;

        try {
            while (true) {
                List<Order> orders = orderRepository.findTop1000ByIdGreaterThanAndCreatedAtBetweenOrderByIdAsc(
                        lastId, startOfYesterday, startOfToday
                );

                if (orders.isEmpty()) break;

                List<OrderHistoryDoc> docs = orders.stream()
                        .map(OrderHistoryDoc::from)
                        .toList();

                orderSearchRepository.saveAll(docs);
                totalSynced += docs.size();

                lastId = orders.get(orders.size() - 1).getId();

                if (orders.size() < batchSize) break;
            }
            log.info("Elasticsearch Sync Complete: {} yesterday's orders synced.", totalSynced);
        } catch (Exception e) {
            log.error("Elasticsearch Sync Failed: {} ", e.getMessage());
            throw new DataArchiveException("Elasticsearch 주문 데이터 동기화 실패", e.getCause());
        }
    }

    private void deleteOldOrders() {
        Instant threshold = Instant.now().minus(3, ChronoUnit.DAYS);
        int batchSize = 1000;
        int totalDeleted = 0;

        try{
            while (true) {
                List<Order> orders = orderRepository.findTop1000ByCreatedAtBefore(threshold);

                if (orders.isEmpty()) break;

                orderRepository.deleteAllInBatch(orders);
                totalDeleted += orders.size();

                if (orders.size() < batchSize) break;
            }
            log.info("DB Cleanup Complete: {} orders older than 3 days deleted.", totalDeleted);
        } catch (Exception e) {
            log.error("DB Cleanup Failed: 3일 경과 주문 데이터 삭제 중 에러 발생. 진행된 삭제 건수: {}", totalDeleted, e);
            throw new DataCleanupException("오래된 주문 DB 삭제 실패", e);
        }
    }
}