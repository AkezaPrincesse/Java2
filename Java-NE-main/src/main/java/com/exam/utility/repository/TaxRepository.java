package com.exam.utility.repository;

import com.exam.utility.entity.Tax;
import com.exam.utility.enums.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxRepository extends JpaRepository<Tax, Long> {
    List<Tax> findByUtilityTypeAndActiveTrue(MeterType utilityType);
    List<Tax> findByActiveTrue();
}
