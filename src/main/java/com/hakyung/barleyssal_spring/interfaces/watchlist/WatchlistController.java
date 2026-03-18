package com.hakyung.barleyssal_spring.interfaces.watchlist;

import com.hakyung.barleyssal_spring.application.watchlist.WatchlistService;
import com.hakyung.barleyssal_spring.application.watchlist.dto.WatchlistItemResponse;
import com.hakyung.barleyssal_spring.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public List<WatchlistItemResponse> getWatchlist(
            @AuthenticationPrincipal CustomUserDetails user) {
        return watchlistService.getWatchlist(user.getId());
    }

    @PostMapping("/{stockCode}")
    public WatchlistItemResponse add(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String stockCode,
            @RequestBody(required = false) Map<String, String> body) {
        String stockName = body != null ? body.getOrDefault("stockName", stockCode) : stockCode;
        return watchlistService.addToWatchlist(user.getId(), stockCode, stockName);
    }

    @DeleteMapping("/{stockCode}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String stockCode) {
        watchlistService.removeFromWatchlist(user.getId(), stockCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{stockCode}/toggle")
    public Map<String, Object> toggle(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String stockCode,
            @RequestBody(required = false) Map<String, String> body) {
        String stockName = body != null ? body.getOrDefault("stockName", stockCode) : stockCode;
        boolean watched = watchlistService.toggle(user.getId(), stockCode, stockName);
        return Map.of("watched", watched, "stockCode", stockCode);
    }

    @GetMapping("/{stockCode}/status")
    public Map<String, Object> status(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String stockCode) {
        boolean watched = watchlistService.isWatched(user.getId(), stockCode);
        return Map.of("watched", watched, "stockCode", stockCode);
    }
}