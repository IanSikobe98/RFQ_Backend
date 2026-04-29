package com.kingdom_bank.RFQBackend.repository;

import com.kingdom_bank.RFQBackend.entity.Order;
import com.kingdom_bank.RFQBackend.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {
    @Query("SELECT r FROM Order r WHERE r.cifAccountCode = :cifAccountCode " +
            "AND ((:accountNumber IS NULL AND r.accountNumber IS NULL) OR r.accountNumber = :accountNumber) " +
            "AND r.fromCurrency = :fromCurrency " +
            "AND r.toCurrency = :toCurrency " +
            "AND r.status NOT IN (:status) " +
            "AND (r.counterNominalAmount = :amount OR ABS(r.counterNominalAmount - :amount) < 0.01) " +
            "AND r.requestDate >= :timeThreshold " +
            "ORDER BY r.requestDate DESC")
    List<Order> findRecentDuplicateRFQs(
            @Param("cifAccountCode") String cifAccountCode,
            @Param("accountNumber") String accountNumber,
            @Param("fromCurrency") String fromCurrency,
            @Param("toCurrency") String toCurrency,
            @Param("amount") BigDecimal amount,
            @Param("timeThreshold") LocalDateTime timeThreshold,
            @Param("status") Status status
    );




    List<Order> findByStatus_StatusIdInOrderByDateAddedDesc(List<Integer> statusId);

    List<Order> findByStatusIn(List<Status> statuses);

    Double countByStatus(Status status);

    List<Order> findByDateApprovedBetweenAndStatus(Date startOfWeek, Date endOfWeek,Status status);

    Optional<Order> findTopByOrderByDealerCodeDesc();



}
