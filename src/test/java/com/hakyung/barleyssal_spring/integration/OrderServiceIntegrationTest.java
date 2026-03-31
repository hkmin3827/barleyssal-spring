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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("OrderService 통합 테스트")
class OrderServiceIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired UserRepository userRepository;

    @MockitoBean RedissonClient redissonClient;
    @MockitoBean OrderEventProducer orderEventProducer;
    @MockitoBean RedisAccountRepository redisAccountRepository;
    @MockitoBean RedisOrderRepository redisOrderRepository;
    @MockitoBean RedisMarketRepository redisMarketRepository;
    @MockitoBean com.hakyung.barleyssal_spring.infrastruture.elastic.TradeStatsRepository tradeStatsRepository;
    @MockitoBean com.hakyung.barleyssal_spring.infrastruture.elastic.TradeStatisticsService tradeStatisticsService;
    @MockitoBean com.hakyung.barleyssal_spring.infrastruture.elastic.OrderSearchRepository orderSearchRepository;

    private Long userId;

    @BeforeEach
    void setUp() throws InterruptedException {
        RLock rLock = mock(RLock.class);
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isLocked()).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        willDoNothing().given(rLock).unlock();

        SignupRequest req = new SignupRequest(
                "integration@test.com", "Test1234!", "통합테스터", "01012345678");
        User user = userRepository.save(User.of(req, "$2a$10$encodedpassword"));
        userId = user.getId();

        accountRepository.save(Account.create(userId, "통합테스터", Money.of(10_000_000L), "ACC-IT-001"));

        given(redisMarketRepository.getMarketOperationCode(any())).willReturn("20");
        given(redisMarketRepository.getCurrentPrice("005930")).willReturn(60_000.0);
        given(redisMarketRepository.getHighPrice("005930")).willReturn(65_000.0);
        given(redisMarketRepository.getCurrentPrice("000660")).willReturn(180_000.0);
        given(redisMarketRepository.getHighPrice("000660")).willReturn(null);
        willDoNothing().given(orderEventProducer).publishOrderCreated(any());
        willDoNothing().given(redisAccountRepository).syncAccountToRedis(any());
    }

    @Nested
    @DisplayName("주문 생성 → DB 저장 검증")
    class CreateAndPersist {

        @Test
        @DisplayName("성공 - 시장가 BUY 주문이 DB에 SUBMITTED 상태로 저장된다")
        void market_buy_saved_to_db() {
            var req = new CreateOrderRequest("005930", OrderSide.BUY, OrderType.MARKET, 1L, null);
            var result = orderService.createOrder(userId, req);

            var saved = orderRepository.findById(result.id());
            assertThat(saved).isPresent();
            assertThat(saved.get().getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
        }

        @Test
        @DisplayName("성공 - 지정가 BUY 주문이 limitPrice 포함해서 DB에 저장된다")
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
        @DisplayName("성공 - JPA 엔티티 매핑 및 Auditing 정상 동작 확인")
        void jpa_entity_mapping_and_auditing_works() {
            var req = new CreateOrderRequest("005930", OrderSide.BUY, OrderType.MARKET, 5L, null);
            Long orderId = orderService.createOrder(userId, req).id();

            Order saved = orderRepository.findById(orderId).orElseThrow();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
        }
    }

    @Nested
    @DisplayName("주문 생성 → deposit 변경 DB 반영 검증")
    class DepositChange {

        @Test
        @DisplayName("성공 - 시장가 BUY: 고가(65,000) * 1주 = 65,000 차감")
        void market_buy_blocks_deposit_in_db() {
            var req = new CreateOrderRequest("005930", OrderSide.BUY, OrderType.MARKET, 1L, null);
            orderService.createOrder(userId, req);

            Account account = accountRepository.findByUserId(userId).orElseThrow();
            assertThat(account.getDeposit()).isEqualByComparingTo("9935000.00");
        }

        @Test
        @DisplayName("성공 - 지정가 BUY: 지정가(180,000) * 2주 = 360,000 차감")
        void limit_buy_blocks_limit_price_x_qty() {
            var req = new CreateOrderRequest("000660", OrderSide.BUY,
                    OrderType.LIMIT, 2L, new BigDecimal("180000"));
            orderService.createOrder(userId, req);

            Account account = accountRepository.findByUserId(userId).orElseThrow();
            assertThat(account.getDeposit()).isEqualByComparingTo("9640000.00");
        }
    }

    @Nested
    @DisplayName("주문 취소 → DB 상태 검증")
    class CancelOrder {

        @Test
        @DisplayName("성공 - 취소 시 주문 CANCELLED + 블로킹 예수금 반환이 DB에 반영된다")
        void cancel_changes_status_and_unblocks_deposit() {
            var req = new CreateOrderRequest("005930", OrderSide.BUY, OrderType.MARKET, 1L, null);
            var created = orderService.createOrder(userId, req);

            BigDecimal depositAfterOrder = accountRepository.findByUserId(userId)
                    .orElseThrow().getDeposit();

            orderService.cancelOrder(userId, created.id());

            Order cancelled = orderRepository.findById(created.id()).orElseThrow();
            assertThat(cancelled.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

            BigDecimal depositAfterCancel = accountRepository.findByUserId(userId)
                    .orElseThrow().getDeposit();
            assertThat(depositAfterCancel).isGreaterThan(depositAfterOrder);
        }
    }

    @Nested
    @DisplayName("에러 케이스 통합 검증")
    class ErrorCases {

        @Test
        @DisplayName("실패 - 장 마감 시 예외 발생 + 예수금/주문 DB 변경 없음")
        void market_closed_throws_no_side_effect() {
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
        @DisplayName("실패 - 예수금 부족 시 예외 발생 + 예수금 변경 없음")
        void insufficient_deposit_throws_and_deposit_unchanged() {
            given(redisMarketRepository.getHighPrice("005930")).willReturn(null);
            given(redisMarketRepository.getCurrentPrice("005930")).willReturn(60_000.0);

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