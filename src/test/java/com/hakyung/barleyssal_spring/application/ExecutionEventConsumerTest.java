package com.hakyung.barleyssal_spring.application;

import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderRepository;
import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.domain.order.OrderStatus;
import com.hakyung.barleyssal_spring.domain.order.OrderType;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import com.hakyung.barleyssal_spring.infrastruture.kafka.DlqService;
import com.hakyung.barleyssal_spring.infrastruture.kafka.ExecutionEventConsumer;
import com.hakyung.barleyssal_spring.infrastruture.kafka.events.ExecutionEvent;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisAccountRepository;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mock.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionEventConsumer 단위 테스트")
class ExecutionEventConsumerTest {

    @InjectMocks ExecutionEventConsumer consumer;
    @Mock OrderRepository orderRepository;
    @Mock AccountRepository accountRepository;
    @Mock(strictness = LENIENT) RedisAccountRepository redisAccountRepository;
    @Mock RedisOrderRepository redisOrderRepository;
    @Mock DlqService dlqService;
    @Mock(strictness = LENIENT) ObjectMapper objectMapper;
    @Mock(strictness = LENIENT) Acknowledgment ack;

    private Account account;
    private Order submittedOrder;

    private static final String EVENT_JSON = """
            {"orderId":"1","userId":"1","userName":"테스터","accountId":"1",
             "stockCode":"005930","orderSide":"BUY","orderType":"MARKET",
             "executedQuantity":"5","executedPrice":62000,"timestamp":1000,"executionStatus":"SUCCESS"}
            """;

    private ExecutionEvent successEvent() {
        return new ExecutionEvent("1", "1", "테스터", "1",
                "005930", "BUY", "MARKET", "5",
                new BigDecimal("62000"), 1000L, "SUCCESS");
    }

    private ExecutionEvent cancelEvent() {
        return new ExecutionEvent("1", "1", "테스터", "1",
                "005930", "BUY", "MARKET", "5",
                new BigDecimal("62000"), 1000L, "CANCELLED");
    }

    @BeforeEach
    void setUp() {
        account = Account.create(1L, "테스터", Money.of(10_000_000L), "ACC-001");

        account.blockDeposit(Money.of(78_000L));

        submittedOrder = Order.create(
                1L,
                StockCode.of("005930"),
                OrderSide.BUY,
                OrderType.MARKET,
                5L,
                null,
                Money.of(78_000L)
        );
        submittedOrder.markSubmitted();

        willDoNothing().given(ack).acknowledge();
        willDoNothing().given(redisAccountRepository).syncAccountToRedis(any());
    }

    private void givenAccountAndOrder() {
        given(accountRepository.findByIdWithLock(1L)).willReturn(Optional.of(account));
        given(orderRepository.findById(1L)).willReturn(Optional.of(submittedOrder));
        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("정상 체결 (SUCCESS)")
    class BuySuccess {

        @BeforeEach
        void mockSetup() throws Exception {
            given(objectMapper.readValue(anyString(), eq(ExecutionEvent.class)))
                    .willReturn(successEvent());
            givenAccountAndOrder();
        }

        @Test
        @DisplayName("성공 - 매수 체결 시 주문 FILLED + 보유주식 추가")
        void order_status_filled_and_holding_created() throws Exception {
            consumer.onExecutionEvent(EVENT_JSON, ack);

            assertThat(submittedOrder.getOrderStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(submittedOrder.getExecutedPrice()).isEqualByComparingTo("62000.00");
            assertThat(submittedOrder.getExecutedQuantity()).isEqualTo(5L);

            assertThat(account.getHoldings()).hasSize(1);
            assertThat(account.getHoldings().get(0).getTotalQuantity()).isEqualTo(5L);

            assertThat(account.getDeposit()).isEqualByComparingTo("9690000.00");

            then(ack).should().acknowledge();
            then(redisAccountRepository).should().syncAccountToRedis(account);
        }
    }

    @Nested
    @DisplayName("주문 취소 (CANCELLED)")
    class CancelProcessing {

        @BeforeEach
        void mockSetup() throws Exception {
            given(objectMapper.readValue(anyString(), eq(ExecutionEvent.class)))
                    .willReturn(cancelEvent());
            givenAccountAndOrder();
        }

        @Test
        @DisplayName("성공 - 취소 이벤트 수신 시 블로킹된 예수금이 반환된다")
        void buy_cancel_unblocks_deposit() throws Exception {
            BigDecimal depositBefore = account.getDeposit();

            consumer.onExecutionEvent(EVENT_JSON, ack);

            assertThat(account.getDeposit())
                    .isGreaterThan(depositBefore)
                    .isEqualByComparingTo("10000000.00");

            assertThat(submittedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            then(ack).should().acknowledge();
        }
    }

    @Nested
    @DisplayName("에러 처리")
    class ErrorHandling {

        @Test
        @DisplayName("실패 - 주문을 찾을 수 없으면 DLQ 전송 후 Ack 처리한다")
        void order_not_found_goes_to_dlq() throws Exception {
            given(objectMapper.readValue(anyString(), eq(ExecutionEvent.class)))
                    .willReturn(successEvent());
            given(orderRepository.findById(1L))
                    .willThrow(new CustomException(ErrorCode.ORDER_NOT_FOUND));
            willDoNothing().given(dlqService).sendToDlq(anyString(), anyString(), anyString(), anyString());
            willDoNothing().given(redisOrderRepository).rollbackOrderToRedis(any(ExecutionEvent.class));

            consumer.onExecutionEvent(EVENT_JSON, ack);

            then(dlqService).should().sendToDlq(
                    eq("execution.event"), any(), eq("ORDER_NOT_FOUND"), eq("BUSINESS_ERROR"));
            then(redisOrderRepository).should().rollbackOrderToRedis(any(ExecutionEvent.class));
            then(ack).should().acknowledge();
        }

        @Test
        @DisplayName("실패 - 일시적 인프라 장애 시 예외를 던져 Kafka 재시도를 유도한다")
        void transient_exception_propagates_for_retry() throws Exception {
            given(objectMapper.readValue(anyString(), eq(ExecutionEvent.class)))
                    .willReturn(successEvent());
            given(orderRepository.findById(1L))
                    .willThrow(new TransientDataAccessException("일시적인 인프라 장애 발생") {});

            assertThatThrownBy(() -> consumer.onExecutionEvent(EVENT_JSON, ack))
                    .isInstanceOf(TransientDataAccessException.class);

            then(ack).should(never()).acknowledge();
        }
    }
}