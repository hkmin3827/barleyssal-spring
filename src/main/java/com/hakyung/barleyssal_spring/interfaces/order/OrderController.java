package com.hakyung.barleyssal_spring.interfaces.order;

import com.hakyung.barleyssal_spring.application.order.OrderService;
import com.hakyung.barleyssal_spring.application.order.dto.CreateOrderRequest;
import com.hakyung.barleyssal_spring.application.order.dto.OrderResponse;
import com.hakyung.barleyssal_spring.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 주문 등록 (매수/매도) */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
        @AuthenticationPrincipal CustomUserDetails user,
        @Valid @RequestBody CreateOrderRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderService.createOrder(user.getId(), req));
    }

    /** 내 주문 이력 */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(orderService.getOrdersByUser(user.getId()));
    }

    /** 개별 주문 조회 */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }
}
