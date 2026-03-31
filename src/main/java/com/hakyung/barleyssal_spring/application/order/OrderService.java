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
import com.hakyung.barleyssal_spring.global.exception.AccountNotFoundException;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import com.hakyung.barleyssal_spring.global.lock.DistributedLock;
import com.hakyung.barleyssal_spring.infrastruture.kafka.OrderEventProducer;
import com.hakyung.barleyssal_spring.infrastruture.kafka.events.OrderCreatedEvent;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisAccountRepository;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisMarketRepository;
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
    private final RedisMarketRepository redisMarketRepository;

    @DistributedLock(key = "'order:user:' + #userId", waitTime = 0, leaseTime = 3)
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest req) {
        Account account = accountRepository.findByUserIdWithLock(userId)
                .orElseThrow(AccountNotFoundException::new);

        StockCode code = StockCode.of(req.stockCode());
        Money limitPrice = req.limitPrice() != null ?  Money.of(req.limitPrice()) : null;
        Money blockedDeposit = Money.ZERO;

        String marketCode = redisMarketRepository.getMarketOperationCode(code.value());

        if (marketCode == null || !marketCode.startsWith("2")) {
            throw new CustomException(ErrorCode.MARKET_CLOSED);
        }

        switch (req.orderSide()) {
            case BUY -> {
                if (req.orderType() == OrderType.LIMIT) {
                    req.validateLimitPrice();
                    blockedDeposit = limitPrice.multiply(req.quantity());
            } else {
                    Double highPrice = redisMarketRepository.getHighPrice(code.value());
                    Double currentPrice = redisMarketRepository.getCurrentPrice(code.value());

                    double safePrice = (highPrice != null && highPrice > currentPrice)
                            ? highPrice : (currentPrice * 1.3);

                    blockedDeposit = Money.of(BigDecimal.valueOf(safePrice)).multiply(req.quantity());
                }
                account.blockDeposit(blockedDeposit);
            }
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
                limitPrice,
                blockedDeposit
        );
        orderRepository.save(order);

        if (req.orderType() == OrderType.LIMIT) {
            redisOrderRepository.saveLimitOrder(order, account.getUserId(), account.getUserName());
        }

        orderEventProducer.publishOrderCreated(OrderCreatedEvent.from(order, userId, account.getUserName()));
        order.markSubmitted();

        redisAccountRepository.syncAccountToRedis(account);
        log.info("Order placed: orderId={} code={} side={}", order.getId(), code, req.orderSide());
        return OrderResponse.from(order);
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Account account = accountRepository.findByUserIdWithLock(userId)
                .orElseThrow(AccountNotFoundException::new);

        Order order = orderRepository.findByIdAndAccountId(orderId, account.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel();

        if (order.getOrderSide() == OrderSide.BUY) {
            account.unblockDeposit(Money.of(order.getBlockedDeposit()));
        } else {
            account.unblockHolding(order.getStockCode(), order.getQuantity());
        }
        if (order.getOrderType() == OrderType.LIMIT) {
            redisOrderRepository.removeLimitOrder(order.getId(), order.getStockCode(), order.getOrderSide());
        }

        redisAccountRepository.syncAccountToRedis(account);

        log.info("Order cancelled successfully: orderId={}", order.getId());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(AccountNotFoundException::new);
        return orderRepository.findByAccountIdAndUserId(account.getId(), userId)
                .stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void validateSell(Account account, StockCode code, long qty) {
        if (!account.canSell(code, qty))
            throw new CustomException(ErrorCode.NOT_ENOUGH_STOCK_QUANTITY_TO_SELL);
    }
}

