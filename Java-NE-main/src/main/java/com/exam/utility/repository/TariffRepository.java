package com.exam.utility.repository;

import com.exam.utility.entity.Tariff;
import com.exam.utility.enums.MeterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {
    List<Tariff> findByUtilityTypeAndActiveTrue(MeterType utilityType);
    List<Tariff> findByUtilityTypeAndActiveTrueAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
        MeterType utilityType, LocalDate date);
    Page<Tariff> findByUtilityType(MeterType utilityType, Pageable pageable);
    Page<Tariff> findByActive(boolean active, Pageable pageable);
}
