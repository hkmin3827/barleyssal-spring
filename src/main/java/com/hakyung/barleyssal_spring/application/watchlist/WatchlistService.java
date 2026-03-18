package com.hakyung.barleyssal_spring.application.watchlist;

import com.hakyung.barleyssal_spring.application.watchlist.dto.WatchlistItemResponse;
import com.hakyung.barleyssal_spring.domain.watchlist.Watchlist;
import com.hakyung.barleyssal_spring.domain.watchlist.WatchlistRepository;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private static final int MAX_WATCHLIST_SIZE = 40;

    private final WatchlistRepository watchlistRepository;

    @Transactional(readOnly = true)
    public List<WatchlistItemResponse> getWatchlist(Long userId) {
        return watchlistRepository.findByUserId(userId)
                .stream()
                .map(WatchlistItemResponse::from)
                .toList();
    }

    @Transactional
    public WatchlistItemResponse addToWatchlist(Long userId, String stockCode, String stockName) {
        if (watchlistRepository.existsByUserIdAndStockCode(userId, stockCode)) {
            return watchlistRepository.findByUserIdAndStockCode(userId, stockCode)
                    .map(WatchlistItemResponse::from)
                    .orElseThrow();
        }

        long count = watchlistRepository.findByUserId(userId).size();
        if (count >= MAX_WATCHLIST_SIZE) {
            throw new CustomException(ErrorCode.WATCHLIST_LIMIT_EXCEEDED);
        }

        Watchlist saved = watchlistRepository.save(
                Watchlist.of(userId, stockCode, stockName)
        );
        return WatchlistItemResponse.from(saved);
    }

    @Transactional
    public void removeFromWatchlist(Long userId, String stockCode) {
        watchlistRepository.deleteByUserIdAndStockCode(userId, stockCode);
    }

    @Transactional
    public boolean toggle(Long userId, String stockCode, String stockName) {
        if (watchlistRepository.existsByUserIdAndStockCode(userId, stockCode)) {
            watchlistRepository.deleteByUserIdAndStockCode(userId, stockCode);
            return false; // 해제됨
        } else {
            long count = watchlistRepository.findByUserId(userId).size();
            if (count >= MAX_WATCHLIST_SIZE) {
                throw new CustomException(ErrorCode.WATCHLIST_LIMIT_EXCEEDED);
            }
            watchlistRepository.save(Watchlist.of(userId, stockCode, stockName));
            return true;
        }
    }

    @Transactional(readOnly = true)
    public boolean isWatched(Long userId, String stockCode) {
        return watchlistRepository.existsByUserIdAndStockCode(userId, stockCode);
    }
}