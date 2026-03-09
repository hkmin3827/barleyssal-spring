package com.hakyung.barleyssal_spring.application.order;

import com.hakyung.barleyssal_spring.application.order.dto.CreateOrderRequest;
import com.hakyung.barleyssal_spring.application.order.dto.OrderResponse;
import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderRepository;
import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.domain.order.OrderType;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import com.hakyung.barleyssal_spring.infrastruture.kafka.OrderEventProducer;
import com.hakyung.barleyssal_spring.infrastruture.kafka.events.OrderCreatedEvent;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisAccountRepository;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final OrderEventProducer orderEventProducer;
    private final RedisOrderRepository redisOrderRepository;
    private final RedisAccountRepository redisAccountRepository;

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest req) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        req.validateLimitPrice();

        StockCode code = StockCode.of(req.stockCode());
        Money limitPrice = req.limitPrice() != null ?  Money.of(req.limitPrice()) : null;

        switch (req.orderSide()) {
            case BUY -> validateBuy(account, req.quantity(), limitPrice, req.orderType());
            case SELL -> {
                validateSell(account, code, req.quantity());
                account.blockHolding(code, req.quantity());
            }
        }
        Order order = Order.create(
                account.getId(),
                code,
                req.orderSide(),
                req.orderType(),
                req.quantity(),
                limitPrice
        );
        orderRepository.save(order);

        if (req.orderType() == OrderType.LIMIT) {
            redisOrderRepository.saveLimitOrder(order);
        }

        orderEventProducer.publishOrderCreated(OrderCreatedEvent.from(order));
        order.markSubmitted();

        redisAccountRepository.syncAccountToRedis(account);
        log.info("Order placed: orderId={} code={} side={}", order.getId(), code, req.orderSide());
        return OrderResponse.from(order);
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        Order order = orderRepository.findByIdAndAccountId(orderId, account.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel();

        if (order.getOrderSide() == OrderSide.BUY) {
            Money refundAmount = Money.of(order.getLimitPrice().multiply(BigDecimal.valueOf(order.getQuantity())));
            account.unblockDeposit(refundAmount);
        } else {
            account.unblockHolding(order.getStockCode(), order.getQuantity());
        }

        if (order.getOrderType() == OrderType.LIMIT) {
            redisOrderRepository.removeLimitOrder(order);
        }

        redisAccountRepository.syncAccountToRedis(account);

        log.info("Order cancelled successfully: orderId={}", order.getId());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        return orderRepository.findByAccountIdAndUserId(account.getId(), userId)
                .stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void validateBuy(Account account, long qty, Money limitPrice, OrderType type) {
        if (type == OrderType.LIMIT && !account.canBuy(qty, limitPrice))
            throw new CustomException(ErrorCode.INSUFFICIENT_DEPOSIT);
    }

    private void validateSell(Account account, StockCode code, long qty) {
        if (!account.canSell(code, qty))
            throw new CustomException(ErrorCode.NOT_ENOUGH_STOCK_QUANTITY_TO_SELL);
    }
}

