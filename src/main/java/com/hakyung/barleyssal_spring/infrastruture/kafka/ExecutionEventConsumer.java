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
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionEventConsumer {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final RedisAccountRepository redisAccountRepository;
    private final RedisOrderRepository redisOrderRepository;
    private final ObjectMapper objectMapper;
    private final DlqService dlqService;

    @Transactional
    @KafkaListener(
            topics      = "execution.event",
            groupId     = "barleyssal-spring",
            concurrency = "1"
    )
    public void onExecutionEvent(String message, Acknowledgment ack) {
        ExecutionEvent event = null;
        try {
            event = objectMapper.readValue(message, ExecutionEvent.class);
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

            redisOrderRepository.removeLimitOrder(order.getId(), order.getStockCode(), order.getOrderSide());

            ack.acknowledge();
            log.info("Asset updated: accountId={} symbol={} side={}", event.accountId(), event.stockCode(), event.orderSide());
        } catch (CustomException e) {
            log.error("데이터 에러 : ", e.getErrorCode());
            dlqService.sendToDlq("execution.event", message, e.getErrorCode().name(), "BUSINESS_ERROR");
            if (event != null) {
                redisOrderRepository.rollbackOrderToRedis(event);
            }

            ack.acknowledge();
        } catch (TransientDataAccessException e) {
            log.error("일시적인 인프라 장애 발생 - 재시도 유도: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("알 수 없는 시스템 에러 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
}
