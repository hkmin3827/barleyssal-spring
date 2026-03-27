package com.hakyung.barleyssal_spring.domain;

import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money VO 단위 테스트")
class MoneyTest {

    @Nested
    @DisplayName("생성")
    class Create {
        @Test
        void of_long() {
            Money m = Money.of(1000L);
            assertThat(m.amount()).isEqualByComparingTo("1000.00");
        }

        @Test
        void of_bigDecimal() {
            Money m = Money.of(new BigDecimal("500.55"));
            assertThat(m.amount()).isEqualByComparingTo("500.55");
        }

        @Test
        void of_string() {
            Money m = Money.of("3000");
            assertThat(m.amount()).isEqualByComparingTo("3000.00");
        }

        @Test
        void null_throws() {
            assertThatThrownBy(() -> Money.of((BigDecimal) null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void ZERO_is_zero() {
            assertThat(Money.ZERO.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("연산")
    class Operations {
        @Test
        void add() {
            Money result = Money.of(1000L).add(Money.of(500L));
            assertThat(result.amount()).isEqualByComparingTo("1500.00");
        }

        @Test
        void subtract_success() {
            Money result = Money.of(1000L).subtract(Money.of(300L));
            assertThat(result.amount()).isEqualByComparingTo("700.00");
        }

        @Test
        void subtract_insufficient_throws() {
            assertThatThrownBy(() -> Money.of(100L).subtract(Money.of(200L)))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void multiply() {
            Money result = Money.of(1000L).multiply(5L);
            assertThat(result.amount()).isEqualByComparingTo("5000.00");
        }

        @Test
        void isGreaterThanOrEqual_true() {
            assertThat(Money.of(1000L).isGreaterThanOrEqual(Money.of(1000L))).isTrue();
            assertThat(Money.of(1001L).isGreaterThanOrEqual(Money.of(1000L))).isTrue();
        }

        @Test
        void isGreaterThanOrEqual_false() {
            assertThat(Money.of(999L).isGreaterThanOrEqual(Money.of(1000L))).isFalse();
        }
    }
}
