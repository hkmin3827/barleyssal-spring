package com.hakyung.barleyssal_spring.domain.holding;

import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "holdings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "stock_code", nullable = false, length = 20))
    private StockCode stockCode;

    @Column(nullable = false)
    private Long totalQuantity;

    private Long blockedQuantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal avgPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    public static Holding create(Account acc, StockCode code, long quantity, Money price) {
        var h = new Holding();
        h.account = acc;
        h.stockCode = code;
        h.totalQuantity = quantity;
        h.avgPrice = price.amount();
        h.blockedQuantity = 0L;
        return h;
    }

    public void addQuantity(long qty, Money price) {
        BigDecimal totalCost = this.avgPrice.multiply(BigDecimal.valueOf(qty))
                .add(price.amount().multiply(BigDecimal.valueOf(qty)));

        this.totalQuantity += qty;
        this.avgPrice = totalCost.divide(BigDecimal.valueOf(this.totalQuantity), 2, RoundingMode.HALF_UP);
    }

    public void subtractQuantity(long qty) {
        if (qty > this.totalQuantity) throw new CustomException(ErrorCode.NOT_ENOUGH_STOCK_QUANTITY_TO_SELL);
        if (qty > this.blockedQuantity) throw new CustomException(ErrorCode.INVALID_EXECUTED_QUANTITY);
        this.totalQuantity -= qty;
        this.blockedQuantity -= qty;
    }

    public void block(long qty) {
        if (this.totalQuantity - this.blockedQuantity < qty)
            throw new CustomException(ErrorCode.NOT_ENOUGH_STOCK_QUANTITY_TO_SELL);
        this.blockedQuantity += qty;
    }

    public void unblock(long qty) {
        if (this.blockedQuantity - qty < 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_QUANTITY);
        }
        this.blockedQuantity -= qty;
    }
    public boolean isEmpty() {
        return this.totalQuantity == 0;
    }
}
