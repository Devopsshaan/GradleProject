package com.ecommerce.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@FeignClient(name = "inventory-service", fallback = InventoryClient.InventoryFallback.class)
public interface InventoryClient {

    @GetMapping("/api/v1/inventory/check")
    @CircuitBreaker(name = "inventory", fallbackMethod = "checkStockFallback")
    List<StockResponse> checkStock(@RequestParam List<String> skus);

    @PostMapping("/api/v1/inventory/reserve")
    @CircuitBreaker(name = "inventory", fallbackMethod = "reserveStockFallback")
    ReservationResponse reserveStock(@RequestBody ReservationRequest request);

    @PostMapping("/api/v1/inventory/release")
    @CircuitBreaker(name = "inventory", fallbackMethod = "releaseStockFallback")
    void releaseStock(@RequestBody ReleaseRequest request);

    @PostMapping("/api/v1/inventory/confirm")
    @CircuitBreaker(name = "inventory", fallbackMethod = "confirmReservationFallback")
    void confirmReservation(@RequestBody ConfirmRequest request);

    // DTOs
    record StockResponse(String sku, int available, boolean inStock) {}
    
    record ReservationRequest(String orderId, List<ReservationItem> items) {}
    record ReservationItem(String sku, int quantity) {}
    record ReservationResponse(String reservationId, boolean success, String message) {}
    
    record ReleaseRequest(String reservationId) {}
    record ConfirmRequest(String reservationId) {}

    // Fallback implementation
    @Component
    class InventoryFallback implements InventoryClient {
        private static final Logger log = LoggerFactory.getLogger(InventoryFallback.class);

        @Override
        public List<StockResponse> checkStock(List<String> skus) {
            log.warn("Fallback: checkStock called for SKUs: {}", skus);
            // Return optimistic response - assume in stock
            return skus.stream()
                    .map(sku -> new StockResponse(sku, 100, true))
                    .toList();
        }

        @Override
        public ReservationResponse reserveStock(ReservationRequest request) {
            log.warn("Fallback: reserveStock called for order: {}", request.orderId());
            return new ReservationResponse(null, false, "Inventory service unavailable");
        }

        @Override
        public void releaseStock(ReleaseRequest request) {
            log.warn("Fallback: releaseStock called for reservation: {}", request.reservationId());
        }

        @Override
        public void confirmReservation(ConfirmRequest request) {
            log.warn("Fallback: confirmReservation called for reservation: {}", request.reservationId());
        }
    }
}
