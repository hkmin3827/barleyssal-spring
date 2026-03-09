package com.hakyung.barleyssal_spring.infrastrutrue.kafka;

import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderRepository;
import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import com.hakyung.barleyssal_spring.infrastrutrue.kafka.events.ExecutionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 체결 결과를 수신하여 DB(Order + Account) 를 원자적으로 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionEventConsumer {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;

    @Transactional
    @KafkaListener(
        topics     = MockMatchingEngineConsumer.TOPIC_EXECUTION_EVENT,
        groupId    = "stocksim-core",
        concurrency = "2"
    )
    public void onExecutionEvent(ExecutionEvent event) {
        log.info("Execution received: orderId={} price={} qty={}",
            event.orderId(), event.executedPrice(), event.executedQuantity());

        try {
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

            log.info("Asset updated: accountId={} symbol={} side={}", event.accountId(), event.stockCode(), event.orderSide());
        } catch (Exception e) {
            log.error("Failed to process ExecutionEvent: orderId={}", event.orderId(), e);


            throw e;  // re-throw → Kafka retry
        }
    }
}
