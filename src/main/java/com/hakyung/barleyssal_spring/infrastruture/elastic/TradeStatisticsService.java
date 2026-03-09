package com.hakyung.barleyssal_spring.infrastruture.elastic;

import com.hakyung.barleyssal_spring.infrastruture.kafka.events.ExecutionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeStatisticsService {
    private final TradeStatsRepository tradeStatsRepository; // 상위 추상화 (Repository)
    private final ObjectMapper objectMapper;

    @jakarta.annotation.PostConstruct
    public void checkBean() {
        log.info("★★★★★ TradeStatisticsService 가 빈으로 등록되었습니다! ★★★★★");
    }

    @KafkaListener(
            topics = "execution.event",
            groupId = "barleyssal-stats-group"
    )
    public void processTradeStats(String message) {
        try {
            log.info("▶▶▶▶▶ [KAFKA 수신 성공] raw message: {}", message);

            ExecutionEvent event = objectMapper.readValue(message, ExecutionEvent.class);
            log.info("Indexing trade stats for ES: orderId={}", event.orderId());
            log.info("▶▶▶▶▶ [매핑 완료] orderId: {}", event.orderId());

            // 1. 통계용 엔티티 변환 (Record 생성자 사용)
            // ID는 null로 넘겨 ES가 자동 생성(Auto-generated ID)
            TradeStatsDoc stats = new TradeStatsDoc(
                    null,
                    Long.valueOf(event.userId()), // ExecutionEvent가 String일 경우 변환
                    event.stockCode(),
                    event.orderSide(),
                    event.executedPrice().doubleValue(),
                    Long.valueOf(event.executedQuantity()),
                    calculateProfitRate(event), // 수익률 계산 로직 필요
                    LocalDateTime.now()
            );

            // 2. ES 인덱싱
            tradeStatsRepository.save(stats);
            log.info("▶▶▶▶▶ [ES 저장 완료]");
            log.debug("TradeStatsDoc indexed: stockCode={} side={}", event.stockCode(), event.orderSide());
        } catch (Exception e) {
            log.error("▶▶▶▶▶ [에러] 데이터 처리 중 예외 발생", e);
        }
    }
    private Double calculateProfitRate(ExecutionEvent event) {
        // 실제 구현 시 유저의 평단가와 체결가를 비교하는 로직 추가
        return 0.0;
    }
}