package com.hakyung.barleyssal_spring.infrastruture.scheduling;

import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderRepository;
import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBulkExpireScheduler {

    private final OrderRepository     orderRepository;
    private final AccountRepository   accountRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    @Scheduled(cron = "0 40 15 * * MON-FRI", zone = "Asia/Seoul")
    public void expireAfterMarketClose() {
        expireOpenOrders("MARKET_CLOSE");
    }

    @Transactional
    public void expireRemaining() {
        expireOpenOrders("MIDNIGHT_FALLBACK");
    }


    private void expireOpenOrders(String trigger) {
        List<Order> openOrders = orderRepository.findByOrderStatusIn(
                List.of(OrderStatus.PENDING, OrderStatus.SUBMITTED)
        );

        if (openOrders.isEmpty()) {
            log.info("[BulkExpire/{}] No open orders. Skipping.", trigger);
            return;
        }

        log.info("[BulkExpire/{}] {} scheduler Processing...", trigger, openOrders.size());

        Set<Long> accountIds = openOrders.stream()
                .map(Order::getAccountId)
                .collect(Collectors.toSet());

        Map<Long, Account> accountMap = accountRepository.findAllById(accountIds)
                .stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        int expiredCount = 0;
        int errorCount   = 0;

        for (Order order : openOrders) {
            try {
                Account account = accountMap.get(order.getAccountId());
                if (account == null) {
                    log.warn("[BulkExpire/{}] Account not found. orderId={} accountId={}. Expiring without refund.",
                             trigger, order.getId(), order.getAccountId());
                    order.expire();
                    errorCount++;
                    continue;
                }

                if (order.getOrderSide() == OrderSide.BUY) {
                    BigDecimal blockedDeposit = order.getBlockedDeposit();
                    if (blockedDeposit != null && blockedDeposit.compareTo(BigDecimal.ZERO) > 0) {
                        account.unblockDeposit(Money.of(blockedDeposit));
                    }

                } else {
                    account.unblockHolding(order.getStockCode(), order.getQuantity());
                }

                order.expire();
                expiredCount++;

            } catch (Exception e) {
                log.error("[BulkExpire/{}] Failed to process orderId={}. Skipping.",
                          trigger, order.getId(), e);
                errorCount++;
            }
        }

        clearRedisKeys("orders:pending:*");
        clearRedisKeys("order:meta:*");

        log.info("[BulkExpire/{}] Done. expired={}, errors={}", trigger, expiredCount, errorCount);
    }

    private void clearRedisKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}