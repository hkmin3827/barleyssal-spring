package com.hakyung.barleyssal_spring.application.watchlist.dto;

import com.hakyung.barleyssal_spring.domain.watchlist.Watchlist;

public record WatchlistItemResponse(
        Long id,
        String stockCode,
        String stockName
) {
    public static WatchlistItemResponse from(Watchlist w) {
        return new WatchlistItemResponse(w.getId(), w.getStockCode(), w.getStockName());
    }
}