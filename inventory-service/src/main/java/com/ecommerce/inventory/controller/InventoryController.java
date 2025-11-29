package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory Management", description = "APIs for managing inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{sku}")
    @Operation(summary = "Get inventory by SKU")
    public ResponseEntity<Inventory> getInventoryBySku(@PathVariable String sku) {
        log.info("GET /api/v1/inventory/{}", sku);
        return inventoryService.findBySku(sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/check")
    @Operation(summary = "Check stock for multiple SKUs")
    public ResponseEntity<List<InventoryService.StockCheckResponse>> checkStock(
            @RequestParam List<String> skus) {
        log.info("GET /api/v1/inventory/check - {} SKUs", skus.size());
        return ResponseEntity.ok(inventoryService.checkStock(skus));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock for an order")
    public ResponseEntity<InventoryService.ReservationResponse> reserveStock(
            @RequestBody ReserveRequest request) {
        log.info("POST /api/v1/inventory/reserve - order: {}", request.orderId());
        
        List<InventoryService.ReservationRequest> items = request.items().stream()
                .map(i -> new InventoryService.ReservationRequest(i.sku(), i.quantity()))
                .toList();
        
        InventoryService.ReservationResponse response = inventoryService.reserveStock(request.orderId(), items);
        
        return response.success() 
                ? ResponseEntity.ok(response) 
                : ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/release")
    @Operation(summary = "Release a stock reservation")
    public ResponseEntity<Void> releaseStock(@RequestBody ReleaseRequest request) {
        log.info("POST /api/v1/inventory/release - reservation: {}", request.reservationId());
        inventoryService.releaseReservation(request.reservationId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm a stock reservation (deduct from inventory)")
    public ResponseEntity<Void> confirmReservation(@RequestBody ConfirmRequest request) {
        log.info("POST /api/v1/inventory/confirm - reservation: {}", request.reservationId());
        inventoryService.confirmReservation(request.reservationId());
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @Operation(summary = "Add new inventory item")
    public ResponseEntity<Inventory> addInventory(@Valid @RequestBody Inventory inventory) {
        log.info("POST /api/v1/inventory - SKU: {}", inventory.getSku());
        Inventory created = inventoryService.addInventory(inventory);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{sku}/stock")
    @Operation(summary = "Update stock quantity")
    public ResponseEntity<Inventory> updateStock(
            @PathVariable String sku,
            @RequestBody StockUpdateRequest request) {
        log.info("PUT /api/v1/inventory/{}/stock - quantity: {}", sku, request.quantity());
        Inventory updated = inventoryService.updateStock(sku, request.quantity());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{sku}/restock")
    @Operation(summary = "Add stock to existing inventory")
    public ResponseEntity<Inventory> restock(
            @PathVariable String sku,
            @RequestBody RestockRequest request) {
        log.info("POST /api/v1/inventory/{}/restock - quantity: {}", sku, request.quantity());
        Inventory updated = inventoryService.restock(sku, request.quantity());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get items with low stock")
    public ResponseEntity<List<Inventory>> getLowStockItems() {
        return ResponseEntity.ok(inventoryService.getLowStockItems());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get inventory statistics")
    public ResponseEntity<InventoryService.InventoryStats> getStats() {
        return ResponseEntity.ok(inventoryService.getStats());
    }

    // Request DTOs
    record ReserveRequest(String orderId, List<ReserveItem> items) {}
    record ReserveItem(String sku, int quantity) {}
    record ReleaseRequest(String reservationId) {}
    record ConfirmRequest(String reservationId) {}
    record StockUpdateRequest(int quantity) {}
    record RestockRequest(int quantity) {}
}
