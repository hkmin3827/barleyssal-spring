package com.hakyung.barleyssal_spring.domain.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUserId(Long userId);

    Optional<Watchlist> findByUserIdAndStockCode(Long userId, String stockCode);

    boolean existsByUserIdAndStockCode(Long userId, String stockCode);

    void deleteByUserIdAndStockCode(Long userId, String stockCode);

    @Modifying
    @Query("DELETE FROM Watchlist wl WHERE wl.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}