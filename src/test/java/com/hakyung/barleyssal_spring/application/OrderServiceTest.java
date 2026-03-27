package com.hakyung.barleyssal_spring.application;

import com.hakyung.barleyssal_spring.application.order.OrderService;
import com.hakyung.barleyssal_spring.application.order.dto.CreateOrderRequest;
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
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisAccountRepository;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisMarketRepository;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mock.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @InjectMocks
    OrderService orderService;
    @Mock
    OrderRepository orderRepository;
    @Mock
    AccountRepository accountRepository;
    @Mock(strictness = LENIENT)
    RedisMarketRepository redisMarketRepository;
    @Mock(strictness = LENIENT)
    OrderEventProducer orderEventProducer;
    @Mock(strictness = LENIENT)
    RedisOrderRepository redisOrderRepository;
    @Mock(strictness = LENIENT)
    RedisAccountRepository redisAccountRepository;

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.create(1L, "테스터", Money.of(10_000_000L), "ACC-001");
        given(accountRepository.findByUserId(1L)).willReturn(Optional.of(account));
        given(redisMarketRepository.getMarketOperationCode(any())).willReturn("20");
        willDoNothing().given(orderEventProducer).publishOrderCreated(any());
        willDoNothing().given(redisAccountRepository).syncAccountToRedis(any());
    }

    private void verifyExternalCallsOnSuccess(boolean isLimitOrder) {
        then(orderEventProducer).should().publishOrderCreated(any());
        then(redisAccountRepository).should().syncAccountToRedis(account);
        if (isLimitOrder) {
            then(redisOrderRepository).should().saveLimitOrder(any(), any(), any());
        } else {
            then(redisOrderRepository).should(never()).saveLimitOrder(any(), any(), any());
        }
    }

    private void verifyNoExternalCallsOnFailure() {
        then(orderEventProducer).should(never()).publishOrderCreated(any());
        then(redisAccountRepository).should(never()).syncAccountToRedis(any());
        then(redisOrderRepository).should(never()).saveLimitOrder(any(), any(), any());
    }

    @Nested
    @DisplayName("시장가 BUY 주문")
    class MarketBuy {
        @Test
        @DisplayName("성공 - 주문 성공 시 고가 기준으로 예수금을 묶는다")
        void success_blocks_deposit_based_on_high_price() {
            given(redisMarketRepository.getHighPrice("005930")).willReturn(65_000.0);
            given(redisMarketRepository.getCurrentPrice("005930")).willReturn(62_000.0);

            CreateOrderRequest req = new CreateOrderRequest("005930", OrderSide.BUY,
                    OrderType.MARKET, 1L, null);

            var result = orderService.createOrder(1L, req);

            assertThat(result).isNotNull();
            assertThat(result.orderSide()).isEqualTo(OrderSide.BUY);
            assertThat(account.getDeposit()).isEqualByComparingTo("9935000.00");

            verifyExternalCallsOnSuccess(false);
        }

        @Test
        @DisplayName("성공 - 받은 고가 데이터가 없을 시 현재가 * 1.3의 가격을 기준으로 예수금을 묶는다")
        void no_high_price_uses_current_price_x13() {
            given(redisMarketRepository.getHighPrice("005930")).willReturn(null);
            given(redisMarketRepository.getCurrentPrice("005930")).willReturn(60_000.0);

            CreateOrderRequest req = new CreateOrderRequest("005930", OrderSide.BUY,
                    OrderType.MARKET, 1L, null);

            orderService.createOrder(1L, req);

            assertThat(account.getDeposit()).isEqualByComparingTo("9922000.00");

            verifyExternalCallsOnSuccess(false);
        }

        @Test
        @DisplayName("실패 - 총 주문 가격보다 예수금이 부족하면 예외를 던진다")
        void insufficient_deposit_throws() {
            given(redisMarketRepository.getHighPrice("005930")).willReturn(null);
            given(redisMarketRepository.getCurrentPrice("005930")).willReturn(60_000.0);

            CreateOrderRequest req = new CreateOrderRequest("005930", OrderSide.BUY,
                    OrderType.MARKET, 200L, null);

            assertThatThrownBy(() -> orderService.createOrder(1L, req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INSUFFICIENT_DEPOSIT);

            verifyNoExternalCallsOnFailure();
        }
    }

    @Nested
    @DisplayName("지정가 BUY 주문")
    class LimitBuy {
        @Test
        @DisplayName("성공 - 주문 성공 시 지정가*수량만큼 예수금을 묶고 레디스에 저장한다")
        void success_blocks_limit_price_x_qty() {
            CreateOrderRequest req = new CreateOrderRequest("000660", OrderSide.BUY,
                    OrderType.LIMIT, 2L, new BigDecimal("30000"));

            orderService.createOrder(1L, req);

            assertThat(account.getDeposit()).isEqualByComparingTo("9940000.00");

            verifyExternalCallsOnSuccess(true);
        }

        @Test
        @DisplayName("실패 - 지정 가격을 입력하지 않으면 예외를 던진다")
        void limit_price_null_throws() {
            CreateOrderRequest req = new CreateOrderRequest("000660", OrderSide.BUY,
                    OrderType.LIMIT, 2L, null);

            assertThatThrownBy(() -> orderService.createOrder(1L, req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.LIMIT_PRICE_REQUIRED);

            verifyNoExternalCallsOnFailure();
        }
    }

    @Nested
    @DisplayName("시장가 SELL 주문")
    class SellOrder {
        @BeforeEach
        void buyFirst() {
            account.processBuy(StockCode.of("005930"), 10L, Money.of(60_000L));
        }

        @Test
        @DisplayName("성공 - 보유 주식에서 매도 가능 수량을 차감한다")
        void sell_marekt_blocks_holding() {
            CreateOrderRequest req = new CreateOrderRequest("005930", OrderSide.SELL,
                    OrderType.MARKET, 5L, null);

            orderService.createOrder(1L, req);

            assertThat(account.canSell(StockCode.of("005930"), 6L)).isFalse();
            assertThat(account.canSell(StockCode.of("005930"), 5L)).isTrue();

            verifyExternalCallsOnSuccess(false);
        }
    }

    @Nested
    @DisplayName("지정가 SELL 주문")
    class LimitSell {
        @BeforeEach
        void buyFirst() {
            account.processBuy(StockCode.of("005930"), 10L, Money.of(60_000L));
        }

        @Test
        @DisplayName("성공 - 보유 주식에서 매도 가능 수량을 차감하고 레디스에 저장한다")
        void sell_limit_blocks_holding() {
            CreateOrderRequest req = new CreateOrderRequest("005930", OrderSide.SELL,
                    OrderType.LIMIT, 5L, new BigDecimal("180000"));

            orderService.createOrder(1L, req);

            assertThat(account.canSell(StockCode.of("005930"), 6L)).isFalse();
            assertThat(account.canSell(StockCode.of("005930"), 5L)).isTrue();

            verifyExternalCallsOnSuccess(true);
        }

        @Test
        @DisplayName("실패 - 보유 주식 수량이 매도 수량보다 적으면 예외를 반환한다")
        void sell_without_holding_throws() {
            CreateOrderRequest req = new CreateOrderRequest("005930", OrderSide.SELL,
                    OrderType.MARKET, 20L, null);

            assertThatThrownBy(() -> orderService.createOrder(1L, req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_ENOUGH_STOCK_QUANTITY_TO_SELL);

            verifyNoExternalCallsOnFailure();
        }
    }

    @Nested
    @DisplayName("장 운영 시간 검증")
    class MarketHours {
        @Test
        @DisplayName("실패 - 장 마감이면 예외를 던진다")
        void market_closed_throws() {
            given(redisMarketRepository.getMarketOperationCode("005930")).willReturn("30");

            CreateOrderRequest req = new CreateOrderRequest("005930", OrderSide.BUY,
                    OrderType.MARKET, 1L, null);

            assertThatThrownBy(() -> orderService.createOrder(1L, req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MARKET_CLOSED);

            verifyNoExternalCallsOnFailure();
        }

        @Test
        @DisplayName("실패 - 장 운영 코드가 없으면 주문 불가로 예외를 던진다")
        void market_code_null_allows_order() {
            given(redisMarketRepository.getMarketOperationCode("005930")).willReturn(null);

            CreateOrderRequest req = new CreateOrderRequest("005930", OrderSide.BUY,
                    OrderType.MARKET, 1L, null);

            assertThatThrownBy(() -> orderService.createOrder(1L, req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MARKET_CLOSED);

            verifyNoExternalCallsOnFailure();
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelOrder {
        @Test
        @DisplayName("성공 - 매수 주문 취소 성공 시 묶었던 예수금을 반환한다")
        void cancel_buy_unblocks_deposit() {
            Order savedOrder = Order.create(account.getId(), StockCode.of("005930"),
                    OrderSide.BUY, OrderType.MARKET, 1L, null, Money.of(78_000L));
            savedOrder.markSubmitted();
            account.blockDeposit(Money.of(78_000L));
            BigDecimal depositAfterOrder = account.getDeposit();

            given(orderRepository.findByIdAndAccountId(any(), any())).willReturn(Optional.of(savedOrder));

            orderService.cancelOrder(1L, 1L);

            assertThat(account.getDeposit()).isGreaterThan(depositAfterOrder);
            assertThat(account.getDeposit()).isEqualByComparingTo("10000000.00");

            then(redisAccountRepository).should().syncAccountToRedis(account);
            then(redisOrderRepository).should(never()).removeLimitOrder(any(), any(), any());
        }

        @Test
        @DisplayName("성공 - 매도 주문 취소 성공 시 묶었던 매도 수량을 매도 가능 수량에 포함한다")
        void cancel_sell_unblocks_holding() {
            account.processBuy(StockCode.of("005930"), 10L, Money.of(60_000L));
            account.blockHolding(StockCode.of("005930"), 3L);

            Long blockedQtybeforeCancel = account.getHoldings().get(0).getBlockedQuantity();

            Order savedOrder = Order.create(account.getId(), StockCode.of("005930"),
                    OrderSide.SELL, OrderType.LIMIT, 3L, Money.of(65_000L), Money.ZERO);
            savedOrder.markSubmitted();

            given(orderRepository.findByIdAndAccountId(any(), any())).willReturn(Optional.of(savedOrder));

            orderService.cancelOrder(1L, 1L);

            assertThat(blockedQtybeforeCancel).isEqualTo(3);
            assertThat(account.getHoldings().get(0).getBlockedQuantity()).isEqualTo(0);

            then(redisAccountRepository).should().syncAccountToRedis(account);
            then(redisOrderRepository).should().removeLimitOrder(any(), any(), any());
        }

        @Test
        @DisplayName("실패 - 이미 취소된 주문일 시 예외를 던진다")
        void cancel_already() {
            Order savedOrder = Order.create(account.getId(), StockCode.of("005930"),
                    OrderSide.SELL, OrderType.LIMIT, 1L, null, Money.ZERO);
            savedOrder.cancel();

            given(orderRepository.findByIdAndAccountId(any(), any())).willReturn(Optional.of(savedOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_CANCELLED_ALREADY);

            then(redisAccountRepository).should(never()).syncAccountToRedis(account);
            then(redisOrderRepository).should(never()).removeLimitOrder(any(), any(), any());
        }
    }
}
