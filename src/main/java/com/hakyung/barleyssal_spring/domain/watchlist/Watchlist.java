package com.hakyung.barleyssal_spring.domain.watchlist;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "watchlists",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "stock_code"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Column(name = "stock_name")
    private String stockName;

    public static Watchlist of(Long userId, String stockCode, String stockName) {
        Watchlist w = new Watchlist();
        w.userId    = userId;
        w.stockCode = stockCode;
        w.stockName = stockName;
        return w;
    }
}