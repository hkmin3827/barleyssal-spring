package com.hakyung.barleyssal_spring.domain;

import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.common.vo.StockCode;
import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderSide;
import com.hakyung.barleyssal_spring.domain.order.OrderStatus;
import com.hakyung.barleyssal_spring.domain.order.OrderType;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order 도메인 단위 테스트")
class OrderTest {
    private Order marketBuyOrder() {
        return Order.create(1L, StockCode.of("005930"), OrderSide.BUY,
                OrderType.MARKET, 5L, null, Money.of(300_000L));
    }

    private Order limitBuyOrder() {
        return Order.create(1L, StockCode.of("000660"), OrderSide.BUY,
                OrderType.LIMIT, 3L, Money.of(180_000L), Money.of(540_000L));
    }

    @Nested
    @DisplayName("주문 생성")
    class Create {
        @Test
        void market_order_initial_status_is_PENDING() {
            Order order = marketBuyOrder();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getOrderType()).isEqualTo(OrderType.MARKET);
            assertThat(order.getOrderSide()).isEqualTo(OrderSide.BUY);
            assertThat(order.getQuantity()).isEqualTo(5L);
        }

        @Test
        void limit_order_has_limit_price() {
            Order order = limitBuyOrder();
            assertThat(order.getLimitPrice()).isEqualByComparingTo("180000.00");
        }

        @Test
        void market_order_has_no_limit_price() {
            Order order = marketBuyOrder();
            assertThat(order.getLimitPrice()).isNull();
        }
    }

    @Nested
    @DisplayName("상태 전환")
    class StatusTransition {
        @Test
        void markSubmitted_from_PENDING() {
            Order order = marketBuyOrder();
            order.markSubmitted();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
        }

        @Test
        void fill_from_SUBMITTED() {
            Order order = marketBuyOrder();
            order.markSubmitted();
            order.fill(Money.of(62_000L), 5L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(order.getExecutedPrice()).isEqualByComparingTo("62000.00");
            assertThat(order.getExecutedQuantity()).isEqualTo(5L);
        }

        @Test
        @DisplayName("실패 - 카프카 이벤트가 발행되지 않은 주문을 체결 시 예외를 던진다")
        void fill_without_submitted_throws() {
            Order order = marketBuyOrder();
            assertThatThrownBy(() -> order.fill(Money.of(62_000L), 5L))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void cancel_from_PENDING() {
            Order order = marketBuyOrder();
            order.cancel();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        void cancel_from_SUBMITTED() {
            Order order = marketBuyOrder();
            order.markSubmitted();
            order.cancel();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("실패 - 이미 체결된 주문을 취소하면 예외를 던진다")
        void cancel_filled_order_throws() {
            Order order = marketBuyOrder();
            order.markSubmitted();
            order.fill(Money.of(62_000L), 5L);

            assertThatThrownBy(order::cancel)
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("실패 - 이미 취소된 주문을 취소하면 예외를 던진다")
        void cancel_already_cancelled_throws() {
            Order order = marketBuyOrder();
            order.cancel();
            assertThatThrownBy(order::cancel)
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void expire_from_SUBMITTED() {
            Order order = marketBuyOrder();
            order.markSubmitted();
            order.expire();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.EXPIRED);
        }

        @Test
        @DisplayName("성공 - 이미 체결된 주문은 만료되지 않는다")
        void expire_from_FILLED_does_nothing() {
            Order order = marketBuyOrder();
            order.markSubmitted();
            order.fill(Money.of(62_000L), 5L);
            order.expire();

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FILLED);
        }
    }
}
