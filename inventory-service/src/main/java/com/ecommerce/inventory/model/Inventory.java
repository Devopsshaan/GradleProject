package com.ecommerce.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_sku", columnList = "sku", unique = true),
    @Index(name = "idx_inventory_warehouse", columnList = "warehouseId")
})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false, length = 50)
    private String sku;

    private Long productId;

    @NotNull
    @Min(0)
    private Integer quantityOnHand = 0;

    @NotNull
    @Min(0)
    private Integer quantityReserved = 0;

    @NotNull
    @Min(0)
    private Integer quantityAvailable = 0;

    @Min(0)
    private Integer reorderPoint = 10;

    @Min(0)
    private Integer reorderQuantity = 50;

    private String warehouseId;
    private String warehouseLocation;

    @Enumerated(EnumType.STRING)
    private InventoryStatus status = InventoryStatus.IN_STOCK;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private LocalDateTime lastRestockedAt;

    @Version
    private Long version;

    // Constructors
    public Inventory() {
        this.createdAt = LocalDateTime.now();
    }

    public Inventory(String sku, Integer quantityOnHand) {
        this();
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.quantityAvailable = quantityOnHand;
        updateStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        updateStatus();
    }

    public void updateStatus() {
        if (quantityAvailable <= 0) {
            this.status = InventoryStatus.OUT_OF_STOCK;
        } else if (quantityAvailable <= reorderPoint) {
            this.status = InventoryStatus.LOW_STOCK;
        } else {
            this.status = InventoryStatus.IN_STOCK;
        }
    }

    public boolean reserve(int quantity) {
        if (quantityAvailable >= quantity) {
            this.quantityReserved += quantity;
            this.quantityAvailable = this.quantityOnHand - this.quantityReserved;
            updateStatus();
            return true;
        }
        return false;
    }

    public void releaseReservation(int quantity) {
        this.quantityReserved = Math.max(0, this.quantityReserved - quantity);
        this.quantityAvailable = this.quantityOnHand - this.quantityReserved;
        updateStatus();
    }

    public void confirmReservation(int quantity) {
        this.quantityOnHand -= quantity;
        this.quantityReserved -= quantity;
        this.quantityAvailable = this.quantityOnHand - this.quantityReserved;
        updateStatus();
    }

    public void restock(int quantity) {
        this.quantityOnHand += quantity;
        this.quantityAvailable = this.quantityOnHand - this.quantityReserved;
        this.lastRestockedAt = LocalDateTime.now();
        updateStatus();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(Integer quantityOnHand) { 
        this.quantityOnHand = quantityOnHand;
        this.quantityAvailable = quantityOnHand - this.quantityReserved;
        updateStatus();
    }

    public Integer getQuantityReserved() { return quantityReserved; }
    public Integer getQuantityAvailable() { return quantityAvailable; }

    public Integer getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(Integer reorderPoint) { this.reorderPoint = reorderPoint; }

    public Integer getReorderQuantity() { return reorderQuantity; }
    public void setReorderQuantity(Integer reorderQuantity) { this.reorderQuantity = reorderQuantity; }

    public String getWarehouseId() { return warehouseId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }

    public String getWarehouseLocation() { return warehouseLocation; }
    public void setWarehouseLocation(String warehouseLocation) { this.warehouseLocation = warehouseLocation; }

    public InventoryStatus getStatus() { return status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastRestockedAt() { return lastRestockedAt; }

    public boolean needsReorder() {
        return quantityAvailable <= reorderPoint;
    }

    public enum InventoryStatus {
        IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    }
}
