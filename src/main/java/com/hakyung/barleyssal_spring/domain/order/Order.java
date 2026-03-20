package com.hakyung.barleyssal_spring.domain.order;

import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_account_created", columnList = "account_id, createdAt DESC"),
        @Index(name = "idx_orders_status_type_created", columnList = "order_status, order_type, createdAt"),
        @Index(name = "idx_orders_stock_code", columnList = "stock_code"),
        @Index(name = "idx_orders_created_at", columnList = "createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "stock_code", nullable = false, length = 20))
    private StockCode stockCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide orderSide;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "limit_price", precision = 19, scale = 2, nullable = true)
    private BigDecimal limitPrice;

    @Column(name = "executed_price", precision = 19, scale = 2)
    private BigDecimal executedPrice;

    @Column(name = "executed_quantity")
    private Long executedQuantity;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    private OrderRejectReason rejectReason;

    @Column(name = "blocked_deposit", precision = 19, scale = 2)
    private BigDecimal blockedDeposit;   // 시장가 고가 기준 필요한 예수금

    @Column(name = "recovery_count", nullable = false)
    private int recoveryCount = 0;

    @Version
    private Long version;

    public static Order create(
            Long accountId,
            StockCode code,
            OrderSide side,
            OrderType type,
            long qty,
            Money limitPrice,
            Money blockedDeposit
    ) {
        var o = new Order();
        o.accountId = accountId;
        o.stockCode = code;
        o.orderSide = side;
        o.orderType = type;
        o.orderStatus = OrderStatus.PENDING;
        o.quantity = qty;
        o.limitPrice = (type == OrderType.LIMIT && limitPrice != null) ? limitPrice.amount() : null;
        o.executedQuantity = 0L;
        o.createdAt = Instant.now();
        o.updatedAt = Instant.now();
        o.blockedDeposit = blockedDeposit != null ? blockedDeposit.amount() : BigDecimal.ZERO;
        return o;
    }

    public void markSubmitted() {
        requireStatus(OrderStatus.PENDING);
        this.orderStatus = OrderStatus.SUBMITTED;
        touch();
    }

    public void fill(Money executedPrice, long qty) {
        requireStatus(OrderStatus.SUBMITTED);
        this.executedPrice = executedPrice.amount();
        this.executedQuantity = qty;
        this.orderStatus = OrderStatus.FILLED;
        touch();
    }

    public void cancel() {
        if(this.orderStatus == OrderStatus.FILLED) throw new CustomException(ErrorCode.ORDER_FILLED_ALREADY);
        if(this.orderStatus == OrderStatus.CANCELLED) {
            throw new CustomException(ErrorCode.ORDER_CANCELLED_ALREADY);
        }

        this.orderStatus = OrderStatus.CANCELLED;
        touch();
    }

    public void reject(OrderRejectReason rejectReason) {
        requireStatus(OrderStatus.PENDING, OrderStatus.SUBMITTED);
        this.orderStatus = OrderStatus.REJECTED;
        this.rejectReason = rejectReason;
        touch();
    }

    public void expire() {
        if (this.orderStatus == OrderStatus.SUBMITTED || this.orderStatus == OrderStatus.PENDING) {
            this.orderStatus = OrderStatus.EXPIRED;
            touch();
        }
    }

    public void incrementRecoveryCount() {
        this.recoveryCount++;
        touch();
    }



    private void requireStatus(OrderStatus... allowed) {
        for (var s : allowed) if (this.orderStatus == s) return;
        throw new CustomException(ErrorCode.UNSUITABLE_ORDER_STATUS);
    }

    private void touch() { this.updatedAt = Instant.now(); }
}
