package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.model.Inventory.InventoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.sku = :sku")
    Optional<Inventory> findBySkuWithLock(@Param("sku") String sku);

    List<Inventory> findBySkuIn(List<String> skus);

    List<Inventory> findByStatus(InventoryStatus status);

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.reorderPoint")
    List<Inventory> findLowStockItems();

    @Query("SELECT i FROM Inventory i WHERE i.warehouseId = :warehouseId")
    List<Inventory> findByWarehouse(@Param("warehouseId") String warehouseId);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.status = :status")
    long countByStatus(@Param("status") InventoryStatus status);

    @Query("SELECT SUM(i.quantityOnHand) FROM Inventory i")
    Long getTotalQuantity();

    @Query("SELECT SUM(i.quantityReserved) FROM Inventory i")
    Long getTotalReserved();

    boolean existsBySku(String sku);
}
