package com.hakyung.barleyssal_spring.interfaces.StatsController;

import com.hakyung.barleyssal_spring.infrastruture.elastic.TradeStatsDoc;
import com.hakyung.barleyssal_spring.infrastruture.elastic.TradeStatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {
    private final TradeStatsQueryService statsQueryService;

    /** 1) 14일 내 단일 매매 손익률 TOP 10 */
    @GetMapping("/top-profitable")
    public ResponseEntity<List<TradeStatsDoc>> getTopProfitableTrades() {
        return ResponseEntity.ok(statsQueryService.findTopProfitableTrades(14));
    }

    /** 2) 14일 내 유저들이 가장 많이 매매한 종목 TOP 10 */
    @GetMapping("/popular-stocks")
    public ResponseEntity<Map<String, Long>> getPopularStocks() {
        return ResponseEntity.ok(statsQueryService.findMostTradedStocks(14));
    }

    /** 3) [관리자] 14일 내 시간대별 Buy/Sell 건수 (차트용) */
    @GetMapping("/admin/hourly-trade-volume")
    public ResponseEntity<Map<String, Object>> getHourlyTradeVolume() {
        return ResponseEntity.ok(statsQueryService.findHourlyTradeVolume(14));
    }

    @GetMapping("/daily-efficiency")
    public ResponseEntity<Map<String, Object>> getDailyEfficiency() {
        return ResponseEntity.ok(statsQueryService.findDailyOrderStats(14));
    }

}