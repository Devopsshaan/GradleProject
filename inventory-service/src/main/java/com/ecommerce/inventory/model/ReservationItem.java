package com.ecommerce.inventory.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "reservation_items")
public class ReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    @JsonIgnore
    private StockReservation reservation;

    private String sku;
    private Integer quantity;

    // Constructors
    public ReservationItem() {}

    public ReservationItem(String sku, Integer quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public StockReservation getReservation() { return reservation; }
    public void setReservation(StockReservation reservation) { this.reservation = reservation; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
