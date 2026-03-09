package com.hakyung.barleyssal_spring.domain.account;

import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.domain.holding.Holding;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "user_id", nullable = false, unique = false)
    private Long userId;

    @Column(name = "principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal principal = BigDecimal.ZERO;

    @Column(name = "total_equity", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalEquity = BigDecimal.ZERO;

    @Column(name = "deposit", nullable = false, precision = 19, scale = 2)
    private BigDecimal deposit = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Holding> holdings =new ArrayList<>();

    public static Account create(Long userId, Money principal, String accountNumber) {
        var acc = new Account();
        acc.userId = userId;
        acc.principal = principal.amount();
        acc.accountNumber = accountNumber;
        acc.totalEquity = principal.amount();
        acc.deposit = principal.amount();
        acc.createdAt = Instant.now();
        acc.updatedAt = Instant.now();
        return acc;
    }

    public void resetPrincipal(Money newPrincipal) {
        this.principal = newPrincipal.amount();
        this.deposit   = newPrincipal.amount();
        this.holdings.clear();  // 원금 재설정 시 계좌 주식 내역 초기화
        touch();
    }

    public void processBuy(StockCode code, long qty, Money price) {
        Money totalPrice = price.multiply(qty);
        Money currentDeposit = Money.of(this.deposit);
        this.deposit = currentDeposit.subtract(totalPrice).amount();

        findHolding(code).ifPresentOrElse(
                h -> h.addQuantity(qty, price),
                        ()-> holdings.add(Holding.create(this, code, qty, price))
        );
        touch();
    }

    public void processSell(StockCode code, long qty, Money price) {
        Holding h = findHolding(code)
                .orElseThrow(() -> new CustomException(ErrorCode.HOLDING_NOT_FOUND));
        h.subtractQuantity(qty);
        if(h.isEmpty()) holdings.remove(h);

        Money proceeds = price.multiply(qty);
        this.deposit = Money.of(this.deposit).add(proceeds).amount();

        touch();
    }

    public boolean canBuy(long qty, Money price) {
        return Money.of(this.deposit).isGreaterThanOrEqual(price.multiply(qty));
    }

    public boolean canSell(StockCode code, long qty) {
        return findHolding(code).map(h -> (h.getTotalQuantity() - h.getBlockedQuantity()) >= qty).orElse(false);
    }

    private Optional<Holding> findHolding(StockCode code) {
        return holdings.stream().filter(h -> h.getStockCode().equals(code)).findFirst();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public void blockHolding(StockCode code, long qty) {
        Holding holding = findHolding(code)
                .orElseThrow(() -> new CustomException(ErrorCode.HOLDING_NOT_FOUND));

        holding.block(qty);
    }

}
