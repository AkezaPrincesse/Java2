package com.exam.utility.repository;

import com.exam.utility.entity.BillingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {
    Optional<BillingCycle> findByBillingYearAndBillingMonth(Integer year, Integer month);
    boolean existsByBillingYearAndBillingMonth(Integer year, Integer month);
}
