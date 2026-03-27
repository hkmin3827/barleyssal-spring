package com.hakyung.barleyssal_spring.integration;

import com.hakyung.barleyssal_spring.application.auth.dto.SignupRequest;
import com.hakyung.barleyssal_spring.application.order.OrderService;
import com.hakyung.barleyssal_spring.application.order.dto.CreateOrderRequest;
import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.order.*;
import com.hakyung.barleyssal_spring.domain.user.User;
import com.hakyung.barleyssal_spring.domain.user.UserRepository;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("OrderService 통합 테스트")
class OrderServiceIntegrationTest {

    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    UserRepository userRepository;

    @MockitoBean
    RedissonClient redissonClient;
    @MockitoBean
    OrderEventProducer orderEventProducer;
    @MockitoBean
    RedisAccountRepository redisAccountRepository;
    @MockitoBean
    RedisOrderRepository redisOrderRepository;
    @MockitoBean
    RedisMarketRepository redisMarketRepository;

    @MockitoBean com.hakyung.barleyssal_spring.infrastruture.elastic.TradeStatsRepository tradeStatsRepository;
    @MockitoBean com.hakyung.barleyssal_spring.infrastruture.elastic.TradeStatisticsService tradeStatisticsService;
    @MockitoBean com.hakyung.barleyssal_spring.infrastruture.elastic.OrderSearchRepository orderSearchRepository;

    private Long userId;

    @BeforeEach
    void setUp() throws InterruptedException {
        SignupRequest req = new SignupRequest(
                "integration@test.com", "Test1234!", "통합테스터", "01012345678");
        User user = User.of(req, "$2a$10$encodedpassword");
        user = userRepository.save(user);
        userId = user.getId();

        Account account = Account.create(userId, "통합테스터", Money.of(10_000_000L), "ACC-IT-001");
        accountRepository.save(account);

        given(redisMarketRepository.getMarketOperationCode(any())).willReturn("20");
        given(redisMarketRepository.getCurrentPrice("005930")).willReturn(60_000.0);
        given(redisMarketRepository.getHighPrice("005930")).willReturn(65_000.0);
        given(redisMarketRepository.getCurrentPrice("000660")).willReturn(180_000.0);
        given(redisMarketRepository.getHighPrice("000660")).willReturn(null);
        willDoNothing().given(orderEventProducer).publishOrderCreated(any());
        willDoNothing().given(redisAccountRepository).syncAccountToRedis(any());

        RLock rLock = mock(RLock.class);

        lenient().doReturn(rLock).when(redissonClient).getLock(anyString());
        lenient().doReturn(true).when(rLock).tryLock(anyLong(), anyLong(), any());
        lenient().doReturn(true).when(rLock).isLocked();
    }

    @Nested
    @DisplayName("매수 주문 생성 -> DB 저장 검증")
    class CreateAndPersist {
        @Test
        @DisplayName("성공 - 시장가 주문 생성 & DB 저장")
        void market_buy_saved_to_db() {
            var req = new CreateOrderRequest("005930", OrderSide.BUY, OrderType.MARKET, 1L, null);

            var result = orderService.createOrder(userId, req);

            assertThat(result).isNotNull();

            var saved = orderRepository.findById(result.id());

            assertThat(saved).isPresent();
            assertThat(saved.get().getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
        }

        @Test
        @DisplayName("성공 - 지정가 주문 생성 & DB 저장(지정가 포함)")
        void limit_buy_saved_with_limit_price() {
            var req = new CreateOrderRequest("000660", OrderSide.BUY,
                    OrderType.LIMIT, 2L, new BigDecimal("180000"));

            var result = orderService.createOrder(userId, req);

            var saved = orderRepository.findById(result.id());

            assertThat(saved).isPresent();
            assertThat(saved.get().getLimitPrice()).isEqualByComparingTo("180000.00");
            then(redisOrderRepository).should().saveLimitOrder(any(), any(), any());
        }

        @Test
        @DisplayName("성공 - 주문 생성 시 JPA 엔티티 매핑과 Auditing이 DB에 정상 반영")
        void jpa_entity_mapping_and_auditing_works() {
            var req = new CreateOrderRequest("005930", OrderSide.BUY, OrderType.MARKET, 5L, null);

            Long orderId = orderService.createOrder(userId, req).id();

            Order savedOrder = orderRepository.findById(orderId).orElseThrow();

            assertThat(savedOrder.getId()).isNotNull();
            assertThat(savedOrder.getCreatedAt()).isNotNull();
            assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
        }
    }

    @Nested
    @DisplayName("주문 생성 -> 계좌 deposit 변경 검증")
    class DepositChange {
        @Test
        @DisplayName("성공 - 시장가 주문은 고가 * 수량으로 차감된 예수금이 db에 반영")
        void buy_blocks_deposit_in_db() {
            var req = new CreateOrderRequest("005930", OrderSide.BUY, OrderType.MARKET, 1L, null);
            orderService.createOrder(userId, req);

            Account account = accountRepository.findByUserId(userId).orElseThrow();

            assertThat(account.getDeposit()).isEqualByComparingTo("9935000.00");
        }

        @Test
        @DisplayName("성공 - 지정가 주문은 지정 금액 * 수량으로 차감된 예수금이 db에 반영")
        void buy_blocks_limit_price_x_qty() {
            var req = new CreateOrderRequest("000660", OrderSide.BUY,
                    OrderType.LIMIT, 2L, new BigDecimal("180000"));
            orderService.createOrder(userId, req);

            Account account = accountRepository.findByUserId(userId).orElseThrow();

            assertThat(account.getDeposit()).isEqualByComparingTo("9640000.00");
        }
    }

    @Nested
    @DisplayName("주문 취소 -> DB 상태 검증")
    class CancelOrder {
        @Test
        @DisplayName("성공 - 주문 취소 시 주문 상태 CANCELLED로 변경, 묶여 있던 예수금이 계좌 예수금에 합산 -> db 반영")
        void cancel_changes_status_and_unblocks_deposit() {
            var req = new CreateOrderRequest("005930", OrderSide.BUY, OrderType.MARKET, 1L, null);
            var created = orderService.createOrder(userId, req);

            Account accountAfterOrder = accountRepository.findByUserId(userId).orElseThrow();
            BigDecimal depositAfterOrder = accountAfterOrder.getDeposit();

            orderService.cancelOrder(userId, created.id());

            var cancelled = orderRepository.findById(created.id()).orElseThrow();
            assertThat(cancelled.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

            Account accountAfterCancel = accountRepository.findByUserId(userId).orElseThrow();
            assertThat(accountAfterCancel.getDeposit()).isGreaterThan(depositAfterOrder);
        }
    }

    @Nested
    @DisplayName("에러 케이스 통합 검증")
    class ErrorCases {
        @Test
        @DisplayName("실패 - 장 마감 시 예외를 던지고 db에 반영되지 않는다")
        void market_closed_throws_and_no_order_saved() {
            given(redisMarketRepository.getMarketOperationCode("005930")).willReturn("30");

            var req = new CreateOrderRequest("005930", OrderSide.BUY, OrderType.MARKET, 1L, null);

            assertThatThrownBy(() -> orderService.createOrder(userId, req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MARKET_CLOSED);

            Account account = accountRepository.findByUserId(userId).orElseThrow();
            assertThat(account.getDeposit()).isEqualByComparingTo("10000000.00");
        }

        @Test
        @DisplayName("실패 - 예수금이 부족한 경우 예외를 던지고 계좌 예수금은 변하지 않는다")
        void insufficient_deposit_throws_and_deposit_unchanged() {
            given(redisMarketRepository.getCurrentPrice("005930")).willReturn(60_000.0);
            given(redisMarketRepository.getHighPrice("005930")).willReturn(null);

            var req = new CreateOrderRequest("005930", OrderSide.BUY, OrderType.MARKET, 200L, null);

            assertThatThrownBy(() -> orderService.createOrder(userId, req))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INSUFFICIENT_DEPOSIT);

            Account account = accountRepository.findByUserId(userId).orElseThrow();
            assertThat(account.getDeposit()).isEqualByComparingTo("10000000.00");
        }
    }


}
