package com.exam.utility.repository;

import com.exam.utility.entity.ServiceCharge;
import com.exam.utility.enums.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceChargeRepository extends JpaRepository<ServiceCharge, Long> {
    List<ServiceCharge> findByUtilityTypeAndActiveTrue(MeterType utilityType);
    List<ServiceCharge> findByActiveTrue();
}
