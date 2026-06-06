package com.exam.utility.repository;

import com.exam.utility.entity.Meter;
import com.exam.utility.enums.MeterStatus;
import com.exam.utility.enums.MeterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterRepository extends JpaRepository<Meter, Long> {
    Optional<Meter> findByMeterNumber(String meterNumber);
    boolean existsByMeterNumber(String meterNumber);

    List<Meter> findByCustomerIdAndStatus(Long customerId, MeterStatus status);
    Page<Meter> findByCustomerId(Long customerId, Pageable pageable);
    Page<Meter> findByStatus(MeterStatus status, Pageable pageable);
    Page<Meter> findByMeterType(MeterType meterType, Pageable pageable);

    @Query("SELECT m FROM Meter m WHERE " +
           "LOWER(m.meterNumber) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "LOWER(m.location) LIKE LOWER(CONCAT('%',:keyword,'%'))")
    Page<Meter> searchMeters(String keyword, Pageable pageable);

    List<Meter> findByStatusAndMeterType(MeterStatus status, MeterType meterType);
    long countByStatus(MeterStatus status);
    long countByMeterType(MeterType meterType);
}
