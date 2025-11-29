package com.ecommerce.product.service;

import com.ecommerce.product.event.ProductEvent;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.model.Product.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final String PRODUCT_TOPIC = "product-events";

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;
    private final Counter productCreatedCounter;
    private final Counter productUpdatedCounter;
    private final Timer productSearchTimer;

    public ProductService(ProductRepository productRepository,
                         KafkaTemplate<String, ProductEvent> kafkaTemplate,
                         MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;

        // Custom metrics
        this.productCreatedCounter = Counter.builder("products.created.total")
                .description("Total products created")
                .register(meterRegistry);

        this.productUpdatedCounter = Counter.builder("products.updated.total")
                .description("Total products updated")
                .register(meterRegistry);

        this.productSearchTimer = Timer.builder("products.search.duration")
                .description("Product search duration")
                .register(meterRegistry);
    }

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        log.debug("Finding product by id: {}", id);
        return productRepository.findById(id);
    }

    @Cacheable(value = "products", key = "'sku-' + #sku")
    @Transactional(readOnly = true)
    public Optional<Product> findBySku(String sku) {
        log.debug("Finding product by SKU: {}", sku);
        return productRepository.findBySku(sku);
    }

    @Transactional(readOnly = true)
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productSearchTimer.record(() -> {
            log.info("Searching products with keyword: {}", keyword);
            return productRepository.searchByKeyword(keyword, pageable);
        });
    }

    @Transactional(readOnly = true)
    public Page<Product> findByCategory(ProductStatus status, String category, Pageable pageable) {
        return productRepository.findByStatusAndCategory(status, category, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByStatusAndPriceRange(ProductStatus.ACTIVE, minPrice, maxPrice, pageable);
    }

    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(Product product) {
        log.info("Creating product: {}", product.getSku());

        if (productRepository.existsBySku(product.getSku())) {
            throw new IllegalArgumentException("Product with SKU " + product.getSku() + " already exists");
        }

        Product saved = productRepository.save(product);
        productCreatedCounter.increment();

        // Publish event
        publishEvent(ProductEvent.created(saved.getId(), saved.getSku(), saved.getName(), saved.getPrice()));

        return saved;
    }

    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "products", key = "'sku-' + #result.sku")
    })
    public Product updateProduct(Long id, Product productDetails) {
        log.info("Updating product: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        BigDecimal oldPrice = product.getPrice();

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setCompareAtPrice(productDetails.getCompareAtPrice());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setTags(productDetails.getTags());
        product.setImageUrls(productDetails.getImageUrls());
        product.setStatus(productDetails.getStatus());
        product.setWeight(productDetails.getWeight());

        Product saved = productRepository.save(product);
        productUpdatedCounter.increment();

        // Publish event
        publishEvent(ProductEvent.updated(saved.getId(), saved.getSku(), saved.getName(), saved.getPrice()));

        // Publish price change event if price changed
        if (oldPrice.compareTo(saved.getPrice()) != 0) {
            publishEvent(ProductEvent.priceChanged(saved.getId(), saved.getSku(), oldPrice, saved.getPrice()));
        }

        return saved;
    }

    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "products", allEntries = true)
    })
    public void deleteProduct(Long id) {
        log.info("Deleting product: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        productRepository.delete(product);

        // Publish event
        publishEvent(ProductEvent.deleted(id, product.getSku()));
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findAllActiveCategories();
    }

    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        return productRepository.findAllActiveBrands();
    }

    @Transactional(readOnly = true)
    public ProductStats getStats() {
        long active = productRepository.countByStatus(ProductStatus.ACTIVE);
        long inactive = productRepository.countByStatus(ProductStatus.INACTIVE);
        long discontinued = productRepository.countByStatus(ProductStatus.DISCONTINUED);
        long outOfStock = productRepository.countByStatus(ProductStatus.OUT_OF_STOCK);
        long total = productRepository.count();

        return new ProductStats(total, active, inactive, discontinued, outOfStock);
    }

    @Transactional(readOnly = true)
    public List<Product> findByIds(List<Long> ids) {
        return productRepository.findByIdIn(ids);
    }

    private void publishEvent(ProductEvent event) {
        try {
            kafkaTemplate.send(PRODUCT_TOPIC, event.getPayload().getSku(), event);
            log.debug("Published event: {} for product: {}", event.getEventType(), event.getPayload().getSku());
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getEventType(), e);
        }
    }

    public record ProductStats(
            long total,
            long active,
            long inactive,
            long discontinued,
            long outOfStock
    ) {}
}
