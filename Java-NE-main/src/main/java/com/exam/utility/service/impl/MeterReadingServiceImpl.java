package com.exam.utility.service.impl;

import com.exam.utility.dto.request.reading.CreateMeterReadingRequest;
import java.math.BigDecimal;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.reading.MeterReadingResponse;
import com.exam.utility.entity.Meter;
import com.exam.utility.entity.MeterReading;
import com.exam.utility.enums.AuditAction;
import com.exam.utility.enums.MeterStatus;
import com.exam.utility.exception.BusinessException;
import com.exam.utility.exception.DuplicateResourceException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.MeterReadingRepository;
import com.exam.utility.repository.MeterRepository;
import com.exam.utility.service.AuditService;
import com.exam.utility.service.MeterReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records and manages meter readings used as the basis for bill calculations.
 *
 * Business rules enforced:
 * - Readings can only be recorded for ACTIVE meters.
 * - Only one reading per meter per month is allowed.
 * - Current reading must be greater than previous reading (no backward meters).
 * - Readings linked to APPROVED or PAID bills cannot be deleted (financial record integrity).
 * - Reading date cannot be in the future (validated at DTO level and enforced here).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeterReadingServiceImpl implements MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterRepository meterRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public MeterReadingResponse create(CreateMeterReadingRequest request) {
        Meter meter = meterRepository.findById(request.getMeterId())
            .orElseThrow(() -> new ResourceNotFoundException("Meter", "id", request.getMeterId()));

        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BusinessException("Cannot record reading for inactive or faulty meter");
        }

        int year = request.getReadingDate().getYear();
        int month = request.getReadingDate().getMonthValue();

        if (meterReadingRepository.existsByMeterIdAndReadingYearAndReadingMonth(meter.getId(), year, month)) {
            throw new DuplicateResourceException(
                "A reading already exists for meter " + meter.getMeterNumber() +
                " for " + year + "/" + month);
        }

        if (request.getCurrentReading() <= request.getPreviousReading()) {
            throw new BusinessException("Current reading must be greater than previous reading");
        }

        MeterReading reading = MeterReading.builder()
            .meter(meter)
            .previousReading(BigDecimal.valueOf(request.getPreviousReading()))
            .currentReading(BigDecimal.valueOf(request.getCurrentReading()))
            .consumption(BigDecimal.valueOf(request.getCurrentReading()).subtract(BigDecimal.valueOf(request.getPreviousReading())))
            .readingDate(request.getReadingDate())
            .readingYear(year)
            .readingMonth(month)
            .notes(request.getNotes())
            .build();

        reading = meterReadingRepository.save(reading);
        auditService.log(AuditAction.CREATE, "MeterReading", reading.getId().toString(),
            "Reading recorded for meter: " + meter.getMeterNumber());
        return toResponse(reading);
    }

    @Override
    @Transactional(readOnly = true)
    public MeterReadingResponse getById(Long id) {
        return toResponse(meterReadingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("MeterReading", "id", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MeterReadingResponse> getByMeter(Long meterId, Pageable pageable) {
        return PagedResponse.of(meterReadingRepository.findByMeterId(meterId, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public MeterReadingResponse getLatestByMeter(Long meterId) {
        return meterReadingRepository.findTopByMeterIdOrderByReadingDateDesc(meterId)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No readings found for meter: " + meterId));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MeterReading reading = meterReadingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("MeterReading", "id", id));

        // Meter readings cannot be modified or deleted after a bill has been approved for them
        if (meterReadingRepository.isReadingLinkedToApprovedBill(id)) {
            throw new BusinessException(
                "Cannot delete a meter reading that has an approved or paid bill. " +
                "Reading integrity must be preserved for financial records.");
        }

        meterReadingRepository.delete(reading);
        auditService.log(AuditAction.DELETE, "MeterReading", id.toString(), "Reading deleted");
    }

    private MeterReadingResponse toResponse(MeterReading r) {
        return MeterReadingResponse.builder()
            .id(r.getId())
            .meterId(r.getMeter().getId())
            .meterNumber(r.getMeter().getMeterNumber())
            .meterType(r.getMeter().getMeterType().name())
            .customerName(r.getMeter().getCustomer().getFullName())
            .previousReading(r.getPreviousReading())
            .currentReading(r.getCurrentReading())
            .consumption(r.getConsumption())
            .readingDate(r.getReadingDate())
            .readingYear(r.getReadingYear())
            .readingMonth(r.getReadingMonth())
            .notes(r.getNotes())
            .createdAt(r.getCreatedAt())
            .createdBy(r.getCreatedBy())
            .build();
    }
}
