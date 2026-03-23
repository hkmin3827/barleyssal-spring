package com.hakyung.barleyssal_spring.infrastruture.scheduling;

import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderRepository;
import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.global.exception.AccountNotFoundException;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRecoveryScheduler {

    private static final int    STALE_MINUTES = 3;
    private static final int    MAX_RECOVERY_ATTEMPTS = 3;
    private static final String ORDER_META_PREFIX     = "order:meta:";

    private final OrderRepository      orderRepository;
    private final AccountRepository    accountRepository;
    private final RedisOrderRepository redisOrderRepository;
    private final StringRedisTemplate  redisTemplate;

    @Transactional
    @Scheduled(fixedDelay = 3 * 60 * 1000)  // 3분 -> 배포 서버 확인 후 가능하면 1분 또는 2분으로 down
    public void recoverStaleOrders() {
        Instant staleThreshold = Instant.now().minus(Duration.ofMinutes(STALE_MINUTES));
        List<Order> candidates = orderRepository.findStaleSubmittedLimitOrders(staleThreshold);

        if (candidates.isEmpty()) return;

        log.info("[OrderRecovery] {} stale SUBMITTED LIMIT orders found", candidates.size());

        int recovered = 0;
        int expired   = 0;

        for (Order order : candidates) {
            try {
                if (order.getRecoveryCount() >= MAX_RECOVERY_ATTEMPTS) {
                    Account account = accountRepository.findById(order.getAccountId())
                            .orElseThrow(AccountNotFoundException::new);

                    if(order.getOrderSide() == OrderSide.BUY) {
                        account.unblockDeposit(Money.of(order.getBlockedDeposit()));
                    } else {
                        account.unblockHolding(order.getStockCode(), order.getQuantity());
                    }
                    log.warn("[OrderRecovery] Giving up. orderId={} exceeded max attempts({})",
                             order.getId(), MAX_RECOVERY_ATTEMPTS);
                    order.expire();
                    expired++;
                    continue;
                }

                String metaKey = ORDER_META_PREFIX + order.getId();
                if (Boolean.TRUE.equals(redisTemplate.hasKey(metaKey))) {
                    continue;
                }

                Account account = accountRepository.findById(order.getAccountId())
                        .orElseThrow(AccountNotFoundException::new);
                redisOrderRepository.saveLimitOrder(order, account.getUserId(), account.getUserName());
                order.incrementRecoveryCount();
                recovered++;

                log.info("[OrderRecovery] Re-registered to Redis. orderId={} stockCode={} side={} attempt={}",
                         order.getId(), order.getStockCode().value(),
                         order.getOrderSide(), order.getRecoveryCount());

            } catch (AccountNotFoundException e) {
                log.warn("[OrderRecovery] Expire Order : Account not found for orderId={}, accountId={}", order.getId(), order.getAccountId());
                order.incrementRecoveryCount();
                order.expire();
                expired++;
            } catch (Exception e) {
                log.error("[OrderRecovery] Unexpected error for orderId={}", order.getId(), e);
            }
        }

        if (recovered > 0 || expired > 0) {
            log.info("[OrderRecovery] Done. recovered={}, expired={}", recovered, expired);
        }
    }
}