package com.ecommerce.order.controller;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.Order.OrderStatus;
import com.ecommerce.order.model.Order.PaymentStatus;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Management", description = "APIs for managing orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieve paginated list of orders")
    public ResponseEntity<Page<Order>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("GET /api/v1/orders - page={}, size={}", page, size);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(orderService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        log.info("GET /api/v1/orders/{}", id);
        return orderService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<Order> getOrderByNumber(@PathVariable String orderNumber) {
        log.info("GET /api/v1/orders/number/{}", orderNumber);
        return orderService.findByOrderNumber(orderNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get orders by customer ID")
    public ResponseEntity<Page<Order>> getOrdersByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/v1/orders/customer/{}", customerId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderService.findByCustomerId(customerId, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status")
    public ResponseEntity<Page<Order>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/v1/orders/status/{}", status);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.findByStatus(status, pageable));
    }

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody Order order) {
        log.info("POST /api/v1/orders - Creating order for customer: {}", order.getCustomerId());
        Order created = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {

        log.info("PATCH /api/v1/orders/{}/status - {}", id, request.status());
        Order updated = orderService.updateStatus(id, request.status());
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/payment")
    @Operation(summary = "Update payment status")
    public ResponseEntity<Order> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody PaymentUpdateRequest request) {

        log.info("PATCH /api/v1/orders/{}/payment - {}", id, request.status());
        Order updated = orderService.updatePaymentStatus(id, request.status(), request.transactionId());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CancelRequest request) {

        log.info("POST /api/v1/orders/{}/cancel", id);
        String reason = request != null ? request.reason() : "Cancelled by customer";
        Order cancelled = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(cancelled);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get order statistics")
    public ResponseEntity<OrderService.OrderStats> getStats() {
        return ResponseEntity.ok(orderService.getStats());
    }

    // Request DTOs
    record StatusUpdateRequest(OrderStatus status) {}
    record PaymentUpdateRequest(PaymentStatus status, String transactionId) {}
    record CancelRequest(String reason) {}
}
