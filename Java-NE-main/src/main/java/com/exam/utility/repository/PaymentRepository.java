package com.exam.utility.repository;

import com.exam.utility.entity.Payment;
import com.exam.utility.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReceiptNumber(String receiptNumber);
    Page<Payment> findByCustomerId(Long customerId, Pageable pageable);
    Page<Payment> findByBillId(Long billId, Pageable pageable);
    List<Payment> findByBillId(Long billId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.bill.id = :billId AND p.paymentStatus = 'COMPLETED'")
    BigDecimal sumPaymentsByBill(Long billId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentStatus = 'COMPLETED' " +
           "AND p.paymentDate BETWEEN :start AND :end")
    BigDecimal sumPaymentsBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT p.paymentMethod, SUM(p.amount) FROM Payment p " +
           "WHERE p.paymentStatus = 'COMPLETED' GROUP BY p.paymentMethod")
    List<Object[]> sumByPaymentMethod();

    long countByPaymentMethod(PaymentMethod method);

    @Query("SELECT p FROM Payment p WHERE " +
           "LOWER(p.receiptNumber) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "LOWER(p.customer.fullName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "LOWER(p.bill.billNumber) LIKE LOWER(CONCAT('%',:keyword,'%'))")
    Page<Payment> searchPayments(@Param("keyword") String keyword, Pageable pageable);
}
