package com.ecommerce.product.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class ProductEvent implements Serializable {

    private String eventId;
    private String eventType;
    private Instant timestamp;
    private ProductPayload payload;

    public ProductEvent() {
        this.timestamp = Instant.now();
        this.eventId = java.util.UUID.randomUUID().toString();
    }

    public ProductEvent(String eventType, ProductPayload payload) {
        this();
        this.eventType = eventType;
        this.payload = payload;
    }

    // Static factory methods
    public static ProductEvent created(Long productId, String sku, String name, BigDecimal price) {
        return new ProductEvent("PRODUCT_CREATED", new ProductPayload(productId, sku, name, price));
    }

    public static ProductEvent updated(Long productId, String sku, String name, BigDecimal price) {
        return new ProductEvent("PRODUCT_UPDATED", new ProductPayload(productId, sku, name, price));
    }

    public static ProductEvent deleted(Long productId, String sku) {
        return new ProductEvent("PRODUCT_DELETED", new ProductPayload(productId, sku, null, null));
    }

    public static ProductEvent priceChanged(Long productId, String sku, BigDecimal oldPrice, BigDecimal newPrice) {
        ProductPayload payload = new ProductPayload(productId, sku, null, newPrice);
        payload.setOldPrice(oldPrice);
        return new ProductEvent("PRICE_CHANGED", payload);
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public ProductPayload getPayload() { return payload; }
    public void setPayload(ProductPayload payload) { this.payload = payload; }

    public static class ProductPayload implements Serializable {
        private Long productId;
        private String sku;
        private String name;
        private BigDecimal price;
        private BigDecimal oldPrice;

        public ProductPayload() {}

        public ProductPayload(Long productId, String sku, String name, BigDecimal price) {
            this.productId = productId;
            this.sku = sku;
            this.name = name;
            this.price = price;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public BigDecimal getOldPrice() { return oldPrice; }
        public void setOldPrice(BigDecimal oldPrice) { this.oldPrice = oldPrice; }
    }
}
