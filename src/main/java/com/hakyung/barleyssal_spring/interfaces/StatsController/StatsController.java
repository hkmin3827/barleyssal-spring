package com.hakyung.barleyssal_spring.interfaces.StatsController;

import com.hakyung.barleyssal_spring.global.ratelimit.RateLimit;
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

    @GetMapping("/top-profitable")
    public ResponseEntity<List<TradeStatsDoc>> getTopProfitableTrades() {
        return ResponseEntity.ok(statsQueryService.findTopProfitableTrades(14));
    }

    @GetMapping("/popular-stocks")
    public ResponseEntity<Map<String, Long>> getPopularStocks() {
        return ResponseEntity.ok(statsQueryService.findMostTradedStocks(14));
    }

    @GetMapping("/admin/hourly-trade-volume")
    public ResponseEntity<Map<String, Object>> getHourlyTradeVolume() {
        return ResponseEntity.ok(statsQueryService.findHourlyTradeVolume(14));
    }

    @GetMapping("/daily-efficiency")
    public ResponseEntity<Map<String, Object>> getDailyEfficiency() {
        return ResponseEntity.ok(statsQueryService.findDailyOrderStats(14));
    }
}