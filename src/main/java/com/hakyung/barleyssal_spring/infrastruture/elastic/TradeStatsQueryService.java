package com.hakyung.barleyssal_spring.infrastruture.elastic;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import com.hakyung.barleyssal_spring.global.utils.TimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeStatsQueryService {
    private final ElasticsearchOperations esOps;

    private static final DateTimeFormatter ES_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");


    public List<TradeStatsDoc> findTopProfitableTrades(int days) {
        if (isIndexEmpty(TradeStatsDoc.class)) return Collections.emptyList();

        try {
            Criteria criteria = new Criteria("timestamp")
                    .greaterThanEqual(LocalDateTime.now().minusDays(days))
                    .and("orderSide").is("SELL");

            org.springframework.data.elasticsearch.core.query.Query query = new CriteriaQuery(criteria)
                    .setPageable(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "finalProfitRate")));

            return esOps.search(query, TradeStatsDoc.class)
                    .stream().map(SearchHit::getContent).toList();
        } catch (Exception e) {
            log.error("Elastic search Error!! findTopProfitableTrades error", e);
            return Collections.emptyList();
        }
    }

    public Map<String, Long> findMostTradedStocks(int days) {
        if (isIndexEmpty(TradeStatsDoc.class)) return Collections.emptyMap();

        String start = TimeConverter.now().minusDays(days).format(ES_DATE_FORMAT);

        Query rangeQuery = Query.of(q -> q.range(r -> r.date(d -> d.field("timestamp").gte(start))));

        NativeQuery query = NativeQuery.builder()
                .withQuery(rangeQuery)
                .withAggregation("popular_stocks", Aggregation.of(a -> a
                        .terms(t -> t.field("stockCode").size(10))))
                .build();
        try {
            SearchHits<TradeStatsDoc> hits = esOps.search(query, TradeStatsDoc.class);
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();

            if (aggs == null) return Map.of();

            return aggs.get("popular_stocks").aggregation().getAggregate().sterms().buckets().array()
                    .stream()
                    .collect(Collectors.toMap(
                            bucket -> bucket.key().stringValue(),
                            StringTermsBucket::docCount,
                            (v1, v2) -> v1,
                            LinkedHashMap::new
                    ));
        } catch (Exception e) {
            log.error("Elastic search Error!! findMostTradedStocks error", e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> findHourlyTradeVolume(int days) {
        if (isIndexEmpty(TradeStatsDoc.class)) return Collections.emptyMap();

        String start = TimeConverter.now().minusDays(days).format(ES_DATE_FORMAT);
        Query rangeQuery = Query.of(q -> q.range(r -> r.date(d -> d.field("timestamp").gte(start))));

        NativeQuery query = NativeQuery.builder()
                .withQuery(rangeQuery)
                .withAggregation("hourly_trades", Aggregation.of(a -> a
                        .dateHistogram(d -> d.field("timestamp").calendarInterval(CalendarInterval.Hour))
                        .aggregations("by_side", sub -> sub.terms(t -> t.field("orderSide")))))
                .build();

        try {
            SearchHits<TradeStatsDoc> hits = esOps.search(query, TradeStatsDoc.class);
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
            if (aggs == null) return Map.of("hourlyData", Collections.emptyList());

            List<Map<String, Object>> result = aggs.get("hourly_trades")
                    .aggregation().getAggregate().dateHistogram().buckets().array()
                    .stream()
                    .map(bucket -> {
                        Map<String, Object> hourBucket = new LinkedHashMap<>();
                        hourBucket.put("hour", bucket.keyAsString());
                        hourBucket.put("count", bucket.docCount());

                        Map<String, Long> bySide = bucket.aggregations().get("by_side")
                                .sterms().buckets().array()
                                .stream()
                                .collect(Collectors.toMap(
                                        s -> s.key().stringValue(),
                                        StringTermsBucket::docCount,
                                        (v1, v2) -> v1,
                                        LinkedHashMap::new
                                ));
                        hourBucket.put("bySide", bySide);
                        return hourBucket;
                    })
                    .collect(Collectors.toList());

            return Map.of("hourlyData", result);
        } catch (Exception e) {
            log.error("Elastic search Error!! findHourlyTradeVolume error", e);
            return Map.of("hourlyData", Collections.emptyList());
        }
    }

    public Map<String, Object> findDailyOrderStats(int days) {
        if (isIndexEmpty(OrderHistoryDoc.class)) return Map.of("dailyEfficiency", Collections.emptyList());

        String start = TimeConverter.now().minusDays(days).format(ES_DATE_FORMAT);
        Query rangeQuery = Query.of(q -> q.range(r -> r.date(d -> d.field("timestamp").gte(start))));

        NativeQuery query = NativeQuery.builder()
                .withQuery(rangeQuery)
                .withAggregation("daily_stats", Aggregation.of(a -> a
                        .dateHistogram(d -> d.field("timestamp").calendarInterval(CalendarInterval.Day))
                        .aggregations("executed_count", sub -> sub.filter(f -> f
                                .range(r -> r.number(n -> n.field("executedQuantity").gt(0.0)))))))
                .build();

        try {
            SearchHits<OrderHistoryDoc> hits = esOps.search(query, OrderHistoryDoc.class);
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
            if (aggs == null) return Map.of("dailyEfficiency", Collections.emptyList());

            List<Map<String, Object>> result = aggs.get("daily_stats")
                    .aggregation().getAggregate().dateHistogram().buckets().array()
                    .stream()
                    .map(bucket -> {
                        Map<String, Object> dayBucket = new LinkedHashMap<>();
                        dayBucket.put("date", bucket.keyAsString());
                        dayBucket.put("totalOrders", bucket.docCount());
                        dayBucket.put("executedOrders",
                                bucket.aggregations().get("executed_count").filter().docCount());
                        return dayBucket;
                    })
                    .collect(Collectors.toList());

            return Map.of("dailyEfficiency", result);
        } catch (Exception e) {
            log.error("Elastic search Error!! findDailyOrderStats error", e);
            return Map.of("dailyEfficiency", Collections.emptyList());
        }
    }

    private boolean isIndexEmpty(Class<?> clazz) {
        try {
            return esOps.count(new CriteriaQuery(new Criteria()), clazz) == 0;
        } catch (Exception e) {
            log.warn("Index for {} might not exist yet: {}", clazz.getSimpleName(), e.getMessage());
            return true;
        }
    }
}