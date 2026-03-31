package com.hakyung.barleyssal_spring.application;

import com.hakyung.barleyssal_spring.application.account.AccountService;
import com.hakyung.barleyssal_spring.application.account.dto.SetPrincipalRequest;
import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountNumberGenerator;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.domain.order.*;
import com.hakyung.barleyssal_spring.global.exception.AccountNotFoundException;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mock.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService 단위 테스트")
class AccountServiceTest {

    @InjectMocks AccountService accountService;
    @Mock AccountRepository accountRepository;
    @Mock AccountNumberGenerator accountNumberGenerator;
    @Mock RedisAccountRepository redisAccountRepository;
    @Mock(strictness = LENIENT)
    OrderRepository orderRepository;

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.create(1L, "테스터", Money.of(100000000L), "ACC-001");
    }

    private Order makeSubmittedOrder(String stockCode) {
        Order order = Order.create(
                account.getId(),
                StockCode.of(stockCode),
                OrderSide.BUY,
                OrderType.MARKET,
                1L,
                null,
                Money.of(78_000L)
        );
        order.markSubmitted();
        return order;
    }

    private Order makePendingOrder(String stockCode) {
        return Order.create(
                account.getId(),
                StockCode.of(stockCode),
                OrderSide.BUY,
                OrderType.LIMIT,
                1L,
                Money.of(60_000L),
                Money.of(60_000L)
        );
    }

    @Nested
    @DisplayName("계좌 생성/조회")
    class GetOrCreate {
        @Test
        @DisplayName("성공 - 기존 계좌가 있으면 조회 결과를 반환한다")
        void returns_existing_account() {
            given(accountRepository.findByUserId(1L)).willReturn(Optional.of(account));

            var result = accountService.getOrCreateAccount(1L, "테스터");

            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(1L);
            then(accountRepository).should(never()).save(any());
            then(redisAccountRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("성공 - 계좌 정보가 없으면 신규 계좌를 생성한다")
        void creates_new_account_when_not_exists() {
            given(accountRepository.findByUserId(1L)).willReturn(Optional.empty());
            given(accountNumberGenerator.generate()).willReturn("ACC-NEW");
            given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            var result = accountService.getOrCreateAccount(1L, "신규");

            assertThat(result).isNotNull();
            then(accountRepository).should().save(any(Account.class));
            then(redisAccountRepository).should().syncAccountToRedis(any());
        }
    }

    @Nested
    @DisplayName("계좌 원금 관리")
    class SetPrincipal {
        @BeforeEach
        void mockSave() {
            lenient().when(accountRepository.findByUserIdWithLock(1L)).thenReturn(Optional.of(account));
            lenient().when(accountRepository.save(any())).thenReturn(account);
        }

        @Test
        @DisplayName("성공 - 원금/예수금이 갱신된다")
        void success_resets_principal_and_deposit() {
            given(orderRepository.findByAccountIdAndOrderStatusIn(any(), anyList()))
                    .willReturn(List.of());

            var result = accountService.setPrincipal(1L, new SetPrincipalRequest(new BigDecimal("5000000")));

            assertThat(result.principal()).isEqualByComparingTo("5000000.00");
            assertThat(result.deposit()).isEqualByComparingTo("5000000.00");
        }

        @Test
        @DisplayName("성공 - SUBMITTED 주문이 있으면 모두 CANCELLED 처리 후 원금 리셋")
        void cancels_submitted_orders_before_reset() {
            Order submitted = makeSubmittedOrder("005930");
            given(orderRepository.findByAccountIdAndOrderStatusIn(any(), anyList()))
                    .willReturn(List.of(submitted));
            given(orderRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            accountService.setPrincipal(1L, new SetPrincipalRequest(new BigDecimal("3000000")));

            assertThat(submitted.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            then(orderRepository).should().saveAll(anyList());
        }

        @Test
        @DisplayName("성공 - PENDING + SUBMITTED 주문 혼재 시 전부 취소")
        void cancels_all_active_orders_mixed_status() {
            Order pending   = makePendingOrder("000660");
            Order submitted = makeSubmittedOrder("005930");
            given(orderRepository.findByAccountIdAndOrderStatusIn(any(), anyList()))
                    .willReturn(List.of(pending, submitted));
            given(orderRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            accountService.setPrincipal(1L, new SetPrincipalRequest(new BigDecimal("1000000")));

            assertThat(pending.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(submitted.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("성공 - 원금 리셋 후 holdings 비워짐 확인")
        void holdings_cleared_after_reset() {
            account.processBuy(StockCode.of("005930"), 5L, Money.of(60_000L));
            assertThat(account.getHoldings()).isNotEmpty();

            given(orderRepository.findByAccountIdAndOrderStatusIn(any(), anyList()))
                    .willReturn(List.of());

            accountService.setPrincipal(1L, new SetPrincipalRequest(new BigDecimal("2000000")));

            assertThat(account.getHoldings()).isEmpty();
            assertThat(account.getDeposit()).isEqualByComparingTo("2000000.00");
        }

        @Test
        @DisplayName("성공 - 주문 취소 후 원금 리셋 순서 보장 (체결이 와도 이미 CANCELLED)")
        void order_cancelled_before_principal_reset() {
            Order submitted = makeSubmittedOrder("005930");
            given(orderRepository.findByAccountIdAndOrderStatusIn(any(), anyList()))
                    .willReturn(List.of(submitted));
            given(orderRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            accountService.setPrincipal(1L, new SetPrincipalRequest(new BigDecimal("5000000")));

            assertThat(submitted.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(account.getPrincipal()).isEqualByComparingTo("5000000.00");
        }

        @Test
        @DisplayName("실패 - 10원 단위가 아니면 예외")
        void not_10_unit_throws() {
            var req = new SetPrincipalRequest(new BigDecimal("5000001"));

            assertThatThrownBy(() -> accountService.setPrincipal(1L, req))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("실패 - 계좌 없으면 AccountNotFoundException")
        void account_not_found_throws() {
            given(accountRepository.findByUserIdWithLock(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    accountService.setPrincipal(99L, new SetPrincipalRequest(new BigDecimal("1000000")))
            ).isInstanceOf(AccountNotFoundException.class);
        }
    }
}