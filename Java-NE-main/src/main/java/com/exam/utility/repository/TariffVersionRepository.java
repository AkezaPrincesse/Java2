package com.exam.utility.repository;

import com.exam.utility.entity.TariffVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TariffVersionRepository extends JpaRepository<TariffVersion, Long> {
    List<TariffVersion> findByTariffIdOrderByTierOrder(Long tariffId);
    List<TariffVersion> findByTariffIdAndEffectiveDateLessThanEqualOrderByTierOrder(
        Long tariffId, LocalDate date);
}
