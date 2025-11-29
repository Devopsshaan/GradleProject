package com.ecommerce.order.service;

import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.Order.OrderStatus;
import com.ecommerce.order.model.Order.PaymentStatus;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String ORDER_TOPIC = "order-events";

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private final Counter ordersCreatedCounter;
    private final Counter ordersCancelledCounter;
    private final Timer orderProcessingTimer;

    public OrderService(OrderRepository orderRepository,
                       InventoryClient inventoryClient,
                       KafkaTemplate<String, Object> kafkaTemplate,
                       MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.kafkaTemplate = kafkaTemplate;

        this.ordersCreatedCounter = Counter.builder("orders.created.total")
                .description("Total orders created")
                .register(meterRegistry);

        this.ordersCancelledCounter = Counter.builder("orders.cancelled.total")
                .description("Total orders cancelled")
                .register(meterRegistry);

        this.orderProcessingTimer = Timer.builder("orders.processing.duration")
                .description("Order processing duration")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional(readOnly = true)
    public Page<Order> findByCustomerId(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> findByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public Order createOrder(Order order) {
        return orderProcessingTimer.record(() -> {
            log.info("Creating order for customer: {}", order.getCustomerId());

            // Check inventory
            List<String> skus = order.getItems().stream()
                    .map(OrderItem::getProductSku)
                    .toList();

            List<InventoryClient.StockResponse> stockResponses = inventoryClient.checkStock(skus);

            // Validate stock
            for (OrderItem item : order.getItems()) {
                boolean inStock = stockResponses.stream()
                        .filter(s -> s.sku().equals(item.getProductSku()))
                        .findFirst()
                        .map(s -> s.available() >= item.getQuantity())
                        .orElse(false);

                if (!inStock) {
                    throw new IllegalStateException("Insufficient stock for SKU: " + item.getProductSku());
                }
            }

            // Reserve inventory
            List<InventoryClient.ReservationItem> reservationItems = order.getItems().stream()
                    .map(item -> new InventoryClient.ReservationItem(item.getProductSku(), item.getQuantity()))
                    .toList();

            InventoryClient.ReservationResponse reservation = inventoryClient.reserveStock(
                    new InventoryClient.ReservationRequest(order.getOrderNumber(), reservationItems)
            );

            if (!reservation.success()) {
                throw new IllegalStateException("Failed to reserve inventory: " + reservation.message());
            }

            // Calculate totals
            order.calculateTotals();

            // Save order
            Order saved = orderRepository.save(order);
            ordersCreatedCounter.increment();

            // Publish event
            publishOrderEvent("ORDER_CREATED", saved);

            log.info("Order created: {}", saved.getOrderNumber());
            return saved;
        });
    }

    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        log.info("Updating order {} status to {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        // Update timestamps based on status
        switch (newStatus) {
            case SHIPPED -> order.setShippedAt(LocalDateTime.now());
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> {
                order.setCancelledAt(LocalDateTime.now());
                ordersCancelledCounter.increment();
                // Release inventory
                // inventoryClient.releaseStock(...)
            }
        }

        Order saved = orderRepository.save(order);
        publishOrderEvent("ORDER_STATUS_CHANGED", saved);

        return saved;
    }

    public Order updatePaymentStatus(Long orderId, PaymentStatus paymentStatus, String transactionId) {
        log.info("Updating payment status for order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setPaymentStatus(paymentStatus);
        order.setPaymentTransactionId(transactionId);

        if (paymentStatus == PaymentStatus.PAID) {
            order.setPaidAt(LocalDateTime.now());
            order.setStatus(OrderStatus.CONFIRMED);
        }

        Order saved = orderRepository.save(order);
        publishOrderEvent("PAYMENT_" + paymentStatus.name(), saved);

        return saved;
    }

    public Order cancelOrder(Long orderId, String reason) {
        log.info("Cancelling order {}: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order that has been shipped or delivered");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setNotes(reason);

        // Release inventory reservation
        // inventoryClient.releaseStock(...)

        Order saved = orderRepository.save(order);
        ordersCancelledCounter.increment();
        publishOrderEvent("ORDER_CANCELLED", saved);

        return saved;
    }

    @Transactional(readOnly = true)
    public OrderStats getStats() {
        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmed = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long processing = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long shipped = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long delivered = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long total = orderRepository.count();

        LocalDateTime now = LocalDateTime.now();
        BigDecimal todayRevenue = orderRepository.sumTotalByStatusAndDateRange(
                List.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED),
                now.toLocalDate().atStartOfDay(),
                now
        );

        return new OrderStats(total, pending, confirmed, processing, shipped, delivered, cancelled,
                todayRevenue != null ? todayRevenue : BigDecimal.ZERO);
    }

    private void publishOrderEvent(String eventType, Order order) {
        try {
            OrderEvent event = new OrderEvent(eventType, order.getOrderNumber(), order.getCustomerId(), 
                    order.getStatus().name(), order.getTotal());
            kafkaTemplate.send(ORDER_TOPIC, order.getOrderNumber(), event);
            log.debug("Published event: {} for order: {}", eventType, order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to publish event: {}", eventType, e);
        }
    }

    public record OrderStats(
            long total,
            long pending,
            long confirmed,
            long processing,
            long shipped,
            long delivered,
            long cancelled,
            BigDecimal todayRevenue
    ) {}

    public record OrderEvent(
            String eventType,
            String orderNumber,
            Long customerId,
            String status,
            BigDecimal total
    ) {}
}
