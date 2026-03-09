package com.hakyung.barleyssal_spring.infrastrutrue.kafka;

import com.hakyung.barleyssal_spring.infrastrutrue.elastic.TradeStatsDoc;
import com.hakyung.barleyssal_spring.infrastrutrue.elastic.TradeStatsRepository;
import com.hakyung.barleyssal_spring.infrastrutrue.kafka.events.ExecutionEvent;
import com.hakyung.barleyssal_spring.infrastrutrue.kafka.events.TradeExecutionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeStatisticsService {

    private final TradeStatsRepository tradeStatsRepository;

    @KafkaListener(
            topics = "execution.event", // 이전에 정의한 체결 토픽
            groupId = "stats-group"
    )
    public void processTradeStats(ExecutionEvent event) {
        log.info("Indexing trade stats for ES: orderId={}", event.orderId());

        // 1. 통계용 엔티티 변환 (Record 생성자 사용)
        // ID는 null로 넘겨 ES가 자동 생성(Auto-generated ID)
        TradeStatsDoc stats = new TradeStatsDoc(
                null,
                Long.valueOf(event.userId()), // ExecutionEvent가 String일 경우 변환
                event.stockCode(),
                event.orderSide(),
                event.executedPrice(),
                Long.valueOf(event.executedQuantity()),
                calculateProfitRate(event), // 수익률 계산 로직 필요
                LocalDateTime.now()
        );

        // 2. ES 인덱싱
        tradeStatsRepository.save(stats);
    }

    private Double calculateProfitRate(ExecutionEvent event) {
        // 실제 구현 시 유저의 평단가와 체결가를 비교하는 로직 추가
        return 0.0;
    }
}