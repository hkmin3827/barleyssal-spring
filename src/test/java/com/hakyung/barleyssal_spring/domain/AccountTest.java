package com.hakyung.barleyssal_spring.domain;

import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account 도메인 단위 테스트")
class AccountTest {

    private Account account;
    private final StockCode SAMSUNG = StockCode.of("005930");

    @BeforeEach
    void setUp() {
        account = Account.create(1L, "테스터", Money.of(1_000_000L), "ACC-001");
    }

    @Nested
    @DisplayName("계좌 생성")
    class CreateAccount {
        @Test
        @DisplayName("성공 - 계좌 생성 시 원금과 예수금은 같아야한다")
        void principal_and_deposit_equal_on_create() {
            assertThat(account.getUserId()).isEqualTo(1L);
            assertThat(account.getUserName()).isEqualTo("테스터");
            assertThat(account.getPrincipal()).isEqualByComparingTo("1000000.00");
            assertThat(account.getDeposit()).isEqualByComparingTo("1000000.00");
        }
    }

    @Nested
    @DisplayName("원금 재설정")
    class ResetPrincipal {
        @Test
        @DisplayName("성공 - 보유 주식을 정리하고 예수금을 원금과 같은 가격으로 갱신한다")
        void reset_clears_holdings_and_updates_deposit() {
            account.processBuy(SAMSUNG, 10L, Money.of(60_000L));
            assertThat(account.getHoldings()).isNotEmpty();

            account.resetPrincipal(Money.of(500_000L));

            assertThat(account.getPrincipal()).isEqualByComparingTo("500000.00");
            assertThat(account.getDeposit()).isEqualByComparingTo("500000.00");
            assertThat(account.getHoldings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("예수금 블로킹")
    class BlockDeposit {
        @Test
        @DisplayName("성공 - 예수금 block 시 계좌 예수금을 차감한다")
        void block_reduces_deposit() {
            account.blockDeposit(Money.of(300_000L));
            assertThat(account.getDeposit()).isEqualByComparingTo("700000.00");
        }

        @Test
        @DisplayName("실패 - block할 금액이 예수금보다 크면 예외를 던진다")
        void block_insufficient_throws() {
            assertThatThrownBy(() -> account.blockDeposit(Money.of(2_000_000L)))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("성공 - 묶어놨던 예수금을 반환한다")
        void unblock_restores_deposit() {
            account.blockDeposit(Money.of(300_000L));
            account.unblockDeposit(Money.of(300_000L));
            assertThat(account.getDeposit()).isEqualByComparingTo("1000000.00");
        }
    }

    @Nested
    @DisplayName("매수 처리")
    class ProcessBuy {
        @Test
        @DisplayName("성공 - 매수 성공 시 해당 종목을 보유 종목에 추가한다")
        void buy_creates_holding() {
            account.processBuy(SAMSUNG, 5L, Money.of(60_000L));

            assertThat(account.getHoldings()).hasSize(1);
            assertThat(account.getHoldings().get(0).getTotalQuantity()).isEqualTo(5L);
            assertThat(account.getDeposit()).isEqualByComparingTo("700000.00");
        }

        @Test
        @DisplayName("성공 - 같은 종목을 매수 시 수량을 합산하고 평균 단가를 계산한다")
        void buy_same_stock_twice_adds_quantity_and_avg_price() {
            account.processBuy(SAMSUNG, 4L, Money.of(60_000L));
            account.processBuy(SAMSUNG, 4L, Money.of(80_000L));

            assertThat(account.getHoldings()).hasSize(1);
            assertThat(account.getHoldings().get(0).getTotalQuantity()).isEqualTo(8L);
            assertThat(account.getHoldings().get(0).getAvgPrice())
                    .isEqualByComparingTo("70000.00");
        }

        @Test
        @DisplayName("성공 - 다른 종목 매수 시 보유 종목이 늘어난다")
        void buy_different_stocks_creates_multiple_holdings() {
            account.processBuy(SAMSUNG, 5L, Money.of(60_000L));
            account.processBuy(StockCode.of("000660"), 3L, Money.of(180_000L));

            assertThat(account.getHoldings()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("매도 처리")
    class ProcessSell {
        @BeforeEach
        void buyFirst() {
            account.processBuy(SAMSUNG, 10L, Money.of(60_000L));
            account.blockHolding(SAMSUNG, 5L);
        }

        @Test
        @DisplayName("성공 - 보유 수량이 줄고 총 매도금액이 예수금에 추가된다")
        void sell_reduces_holding_and_increases_deposit() {
            double depositBefore = account.getDeposit().doubleValue();
            account.processSell(SAMSUNG, 5L, Money.of(70_000L));

            assertThat(account.getHoldings().get(0).getTotalQuantity()).isEqualTo(5L);
            assertThat(account.getDeposit().doubleValue())
                    .isGreaterThan(depositBefore);
        }

        @Test
        @DisplayName("성공 - 전 수량 매도 시 보유 종목에서 제거된다")
        void sell_all_removes_holding() {
            account.blockHolding(SAMSUNG, 5L);
            account.processSell(SAMSUNG, 5L, Money.of(70_000L));
            account.processSell(SAMSUNG, 5L, Money.of(70_000L));

            assertThat(account.getHoldings()).isEmpty();
        }

        @Test
        @DisplayName("실패 - 보유하지 않은 종목일 시 예외를 던진다")
        void sell_without_holding_throws() {
            assertThatThrownBy(() ->
                    account.processSell(StockCode.of("000270"), 1L, Money.of(100_000L))
            ).isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("보유주식 블로킹")
    class BlockHolding {
        @BeforeEach
        void buyFirst() {
            account.processBuy(SAMSUNG, 10L, Money.of(60_000L));
        }

        @Test
        @DisplayName("성공 - 전체 수량에서 블로킹된 수량을 제외한 수량만큼 매도 가능 수량이 계산된다")
        void block_reduces_available_quantity() {
            account.blockHolding(SAMSUNG, 3L);
            assertThat(account.canSell(SAMSUNG, 7L)).isTrue();
            assertThat(account.canSell(SAMSUNG, 8L)).isFalse();
        }

        @Test
        @DisplayName("실패 - 전체 수량보다 블로킹된 수량이 초과하면 예외를 던진다")
        void block_exceeds_quantity_throws() {
            assertThatThrownBy(() -> account.blockHolding(SAMSUNG, 11L))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("성공 - 묶여있던 수량이 풀리면 다시 매도 가능 수량에 합산된다")
        void unblock_restores_available() {
            account.blockHolding(SAMSUNG, 5L);
            account.unblockHolding(SAMSUNG, 5L);
            assertThat(account.canSell(SAMSUNG, 10L)).isTrue();
        }
    }
}
