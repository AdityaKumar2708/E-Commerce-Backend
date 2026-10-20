package com.rkind.ecommerce.order.ccontroller;

import com.rkind.ecommerce.order.dto.response.OrderResponse;
import com.rkind.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @PatchMapping("/{id}/accept")
    public ResponseEntity<OrderResponse> acceptOrder(
            @PathVariable Long id) {

        OrderResponse response = orderService.acceptOrder(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PatchMapping("/{id}/packed")
    public ResponseEntity<OrderResponse> markOrderPacked(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                orderService.markOrderPacked(id)
        );
    }

    @PatchMapping("/{id}/picked-up")
    public ResponseEntity<OrderResponse> markOrderPickedUp(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                orderService.markOrderPickedUp(id)
        );
    }
}