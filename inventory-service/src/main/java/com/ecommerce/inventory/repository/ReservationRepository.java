package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.model.StockReservation;
import com.ecommerce.inventory.model.StockReservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByReservationId(String reservationId);

    Optional<StockReservation> findByOrderId(String orderId);

    List<StockReservation> findByStatus(ReservationStatus status);

    @Query("SELECT r FROM StockReservation r WHERE r.status = :status AND r.expiresAt < :now")
    List<StockReservation> findExpiredReservations(
            @Param("status") ReservationStatus status,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(r) FROM StockReservation r WHERE r.status = :status")
    long countByStatus(@Param("status") ReservationStatus status);
}
