package com.exam.utility.repository;

import com.exam.utility.entity.Bill;
import com.exam.utility.enums.BillStatus;
import com.exam.utility.enums.MeterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByBillNumber(String billNumber);
    Page<Bill> findByCustomerId(Long customerId, Pageable pageable);
    Page<Bill> findByStatus(BillStatus status, Pageable pageable);
    Page<Bill> findByCustomerIdAndStatus(Long customerId, BillStatus status, Pageable pageable);
    Page<Bill> findByUtilityType(MeterType type, Pageable pageable);

    boolean existsByMeterIdAndBillingCycleId(Long meterId, Long billingCycleId);
    List<Bill> findByMeterIdAndBillingCycleId(Long meterId, Long billingCycleId);

    @Query("SELECT b FROM Bill b WHERE b.status NOT IN ('PAID','CANCELLED') AND b.dueDate < :today")
    List<Bill> findOverdueBills(LocalDate today);

    @Query("SELECT SUM(b.totalAmount) FROM Bill b WHERE b.status != 'CANCELLED'")
    BigDecimal sumTotalBilled();

    @Query("SELECT SUM(b.paidAmount) FROM Bill b")
    BigDecimal sumTotalPaid();

    @Query("SELECT SUM(b.balanceAmount) FROM Bill b WHERE b.status NOT IN ('PAID','CANCELLED')")
    BigDecimal sumOutstandingBalance();

    @Query("SELECT COUNT(b) FROM Bill b WHERE b.status = :status")
    long countByStatus(BillStatus status);

    @Query("SELECT b FROM Bill b WHERE b.billingCycle.id = :cycleId")
    List<Bill> findByBillingCycleId(Long cycleId);

    @Query("SELECT b FROM Bill b WHERE " +
           "LOWER(b.billNumber) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "LOWER(b.customer.email) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "LOWER(b.meter.meterNumber) LIKE LOWER(CONCAT('%',:keyword,'%'))")
    Page<Bill> searchBills(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Bill b WHERE b.status NOT IN ('PAID','CANCELLED') " +
           "AND b.customer.id = :customerId ORDER BY b.dueDate ASC")
    List<Bill> findUnpaidBillsByCustomer(Long customerId);
}
