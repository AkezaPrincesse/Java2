package com.exam.utility.repository;

import com.exam.utility.entity.Penalty;
import com.exam.utility.enums.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    List<Penalty> findByUtilityTypeAndActiveTrue(MeterType utilityType);
}
