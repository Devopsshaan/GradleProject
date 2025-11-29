package com.ecommerce.inventory.service;

import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.Inventory.InventoryStatus;
import com.ecommerce.inventory.model.ReservationItem;
import com.ecommerce.inventory.model.StockReservation;
import com.ecommerce.inventory.model.StockReservation.ReservationStatus;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.ReservationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    
    private final Counter reservationsCreated;
    private final Counter reservationsConfirmed;
    private final Counter reservationsReleased;
    private final AtomicLong lowStockCount = new AtomicLong(0);

    public InventoryService(InventoryRepository inventoryRepository,
                           ReservationRepository reservationRepository,
                           MeterRegistry meterRegistry) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;

        this.reservationsCreated = Counter.builder("inventory.reservations.created")
                .description("Total reservations created")
                .register(meterRegistry);

        this.reservationsConfirmed = Counter.builder("inventory.reservations.confirmed")
                .description("Total reservations confirmed")
                .register(meterRegistry);

        this.reservationsReleased = Counter.builder("inventory.reservations.released")
                .description("Total reservations released")
                .register(meterRegistry);

        Gauge.builder("inventory.low_stock.count", lowStockCount, AtomicLong::get)
                .description("Number of low stock items")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public Optional<Inventory> findBySku(String sku) {
        return inventoryRepository.findBySku(sku);
    }

    @Transactional(readOnly = true)
    public List<StockCheckResponse> checkStock(List<String> skus) {
        log.info("Checking stock for {} SKUs", skus.size());
        List<Inventory> inventories = inventoryRepository.findBySkuIn(skus);

        return skus.stream()
                .map(sku -> {
                    Optional<Inventory> inv = inventories.stream()
                            .filter(i -> i.getSku().equals(sku))
                            .findFirst();
                    
                    return inv.map(i -> new StockCheckResponse(
                            sku, 
                            i.getQuantityAvailable(), 
                            i.getQuantityAvailable() > 0
                    )).orElse(new StockCheckResponse(sku, 0, false));
                })
                .toList();
    }

    public ReservationResponse reserveStock(String orderId, List<ReservationRequest> items) {
        log.info("Creating reservation for order: {}", orderId);

        // Check if all items are available
        for (ReservationRequest item : items) {
            Inventory inventory = inventoryRepository.findBySkuWithLock(item.sku())
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + item.sku()));

            if (inventory.getQuantityAvailable() < item.quantity()) {
                return new ReservationResponse(null, false, 
                        "Insufficient stock for SKU: " + item.sku() + 
                        " (available: " + inventory.getQuantityAvailable() + 
                        ", requested: " + item.quantity() + ")");
            }
        }

        // Create reservation
        StockReservation reservation = new StockReservation(orderId);

        for (ReservationRequest item : items) {
            Inventory inventory = inventoryRepository.findBySkuWithLock(item.sku()).get();
            inventory.reserve(item.quantity());
            inventoryRepository.save(inventory);

            ReservationItem reservationItem = new ReservationItem(item.sku(), item.quantity());
            reservation.addItem(reservationItem);
        }

        reservationRepository.save(reservation);
        reservationsCreated.increment();

        log.info("Reservation created: {} for order: {}", reservation.getReservationId(), orderId);
        return new ReservationResponse(reservation.getReservationId(), true, "Stock reserved successfully");
    }

    public void releaseReservation(String reservationId) {
        log.info("Releasing reservation: {}", reservationId);

        StockReservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Reservation is not active: " + reservationId);
        }

        for (ReservationItem item : reservation.getItems()) {
            Inventory inventory = inventoryRepository.findBySkuWithLock(item.getSku())
                    .orElseThrow(() -> new IllegalStateException("SKU not found: " + item.getSku()));
            
            inventory.releaseReservation(item.getQuantity());
            inventoryRepository.save(inventory);
        }

        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReleasedAt(LocalDateTime.now());
        reservationRepository.save(reservation);
        reservationsReleased.increment();

        log.info("Reservation released: {}", reservationId);
    }

    public void confirmReservation(String reservationId) {
        log.info("Confirming reservation: {}", reservationId);

        StockReservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Reservation is not active: " + reservationId);
        }

        for (ReservationItem item : reservation.getItems()) {
            Inventory inventory = inventoryRepository.findBySkuWithLock(item.getSku())
                    .orElseThrow(() -> new IllegalStateException("SKU not found: " + item.getSku()));
            
            inventory.confirmReservation(item.getQuantity());
            inventoryRepository.save(inventory);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        reservationRepository.save(reservation);
        reservationsConfirmed.increment();

        log.info("Reservation confirmed: {}", reservationId);
    }

    public Inventory addInventory(Inventory inventory) {
        log.info("Adding inventory for SKU: {}", inventory.getSku());
        return inventoryRepository.save(inventory);
    }

    public Inventory updateStock(String sku, int quantity) {
        log.info("Updating stock for SKU: {} to {}", sku, quantity);
        
        Inventory inventory = inventoryRepository.findBySkuWithLock(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));
        
        inventory.setQuantityOnHand(quantity);
        return inventoryRepository.save(inventory);
    }

    public Inventory restock(String sku, int quantity) {
        log.info("Restocking SKU: {} with {}", sku, quantity);
        
        Inventory inventory = inventoryRepository.findBySkuWithLock(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));
        
        inventory.restock(quantity);
        return inventoryRepository.save(inventory);
    }

    @Transactional(readOnly = true)
    public List<Inventory> getLowStockItems() {
        return inventoryRepository.findLowStockItems();
    }

    @Transactional(readOnly = true)
    public InventoryStats getStats() {
        long inStock = inventoryRepository.countByStatus(InventoryStatus.IN_STOCK);
        long lowStock = inventoryRepository.countByStatus(InventoryStatus.LOW_STOCK);
        long outOfStock = inventoryRepository.countByStatus(InventoryStatus.OUT_OF_STOCK);
        Long totalQuantity = inventoryRepository.getTotalQuantity();
        Long totalReserved = inventoryRepository.getTotalReserved();
        long activeReservations = reservationRepository.countByStatus(ReservationStatus.ACTIVE);

        return new InventoryStats(
                inStock, lowStock, outOfStock,
                totalQuantity != null ? totalQuantity : 0,
                totalReserved != null ? totalReserved : 0,
                activeReservations
        );
    }

    // Scheduled task to release expired reservations
    @Scheduled(fixedRate = 60000) // Every minute
    public void releaseExpiredReservations() {
        List<StockReservation> expired = reservationRepository.findExpiredReservations(
                ReservationStatus.ACTIVE, LocalDateTime.now());

        for (StockReservation reservation : expired) {
            try {
                log.info("Releasing expired reservation: {}", reservation.getReservationId());
                releaseReservation(reservation.getReservationId());
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);
            } catch (Exception e) {
                log.error("Failed to release expired reservation: {}", reservation.getReservationId(), e);
            }
        }

        // Update low stock gauge
        lowStockCount.set(inventoryRepository.countByStatus(InventoryStatus.LOW_STOCK));
    }

    // DTOs
    public record StockCheckResponse(String sku, int available, boolean inStock) {}
    public record ReservationRequest(String sku, int quantity) {}
    public record ReservationResponse(String reservationId, boolean success, String message) {}
    public record InventoryStats(
            long inStock, long lowStock, long outOfStock,
            long totalQuantity, long totalReserved, long activeReservations
    ) {}
}
