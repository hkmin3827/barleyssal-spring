package com.hakyung.barleyssal_spring.infrastruture.kafka;

import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderRepository;
import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import com.hakyung.barleyssal_spring.infrastruture.kafka.events.ExecutionEvent;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * 체결 결과를 수신하여 DB(Order + Account) 를 원자적으로 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionEventConsumer {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final RedisAccountRepository redisAccountRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(
            topics      = "execution.event",
            groupId     = "barleyssal-spring",
            concurrency = "2"
    )
    public void onExecutionEvent(String message) {

        try {
            ExecutionEvent event = objectMapper.readValue(message, ExecutionEvent.class);
            log.info("Execution received: orderId={} price={} qty={}",
                    event.orderId(), event.executedPrice(), event.executedQuantity());

            Order order = orderRepository.findById(Long.valueOf(event.orderId()))
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

            Money execPrice = Money.of(event.executedPrice());

            order.fill(execPrice, Long.valueOf(event.executedQuantity()));
            orderRepository.save(order);

            Account account = accountRepository.findById(Long.valueOf(event.accountId()))
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

            StockCode symbol = StockCode.of(event.stockCode());
            if (OrderSide.BUY.name().equals(event.orderSide())) {
                account.processBuy(symbol, Long.valueOf(event.executedQuantity()), execPrice);
            } else {
                account.processSell(symbol, Long.valueOf(event.executedQuantity()), execPrice);
            }
            accountRepository.save(account);

            redisAccountRepository.syncAccountToRedis(account);

            log.info("Asset updated: accountId={} symbol={} side={}", event.accountId(), event.stockCode(), event.orderSide());
        } catch (Exception e) {
            log.error("메시지 처리 실패", e);
            throw e;  // re-throw → Kafka retry
        }
    }
}
