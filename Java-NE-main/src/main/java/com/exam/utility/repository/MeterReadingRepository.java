package com.exam.utility.repository;

import com.exam.utility.entity.MeterReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    Optional<MeterReading> findByMeterIdAndReadingYearAndReadingMonth(
        Long meterId, Integer year, Integer month);

    boolean existsByMeterIdAndReadingYearAndReadingMonth(Long meterId, Integer year, Integer month);

    Page<MeterReading> findByMeterId(Long meterId, Pageable pageable);

    Optional<MeterReading> findTopByMeterIdOrderByReadingDateDesc(Long meterId);

    @Query("SELECT mr FROM MeterReading mr WHERE mr.meter.id = :meterId " +
           "ORDER BY mr.readingYear DESC, mr.readingMonth DESC")
    List<MeterReading> findRecentReadings(Long meterId, Pageable pageable);

    List<MeterReading> findByReadingYearAndReadingMonth(Integer year, Integer month);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Bill b " +
           "WHERE b.meterReading.id = :readingId AND b.status IN ('APPROVED','PAID')")
    boolean isReadingLinkedToApprovedBill(Long readingId);
}
