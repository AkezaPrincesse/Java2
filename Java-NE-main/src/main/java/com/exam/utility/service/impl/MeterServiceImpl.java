package com.exam.utility.service.impl;

import java.math.BigDecimal;
import com.exam.utility.dto.request.meter.CreateMeterRequest;
import com.exam.utility.dto.request.meter.UpdateMeterRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.meter.MeterResponse;
import com.exam.utility.entity.Customer;
import com.exam.utility.entity.Meter;
import com.exam.utility.entity.Tariff;
import com.exam.utility.enums.AuditAction;
import com.exam.utility.enums.CustomerStatus;
import com.exam.utility.enums.MeterStatus;
import com.exam.utility.exception.BusinessException;
import com.exam.utility.exception.DuplicateResourceException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.CustomerRepository;
import com.exam.utility.repository.MeterRepository;
import com.exam.utility.repository.TariffRepository;
import com.exam.utility.service.AuditService;
import com.exam.utility.service.MeterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterServiceImpl implements MeterService {

    private final MeterRepository meterRepository;
    private final CustomerRepository customerRepository;
    private final TariffRepository tariffRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public MeterResponse create(CreateMeterRequest request) {
        if (meterRepository.existsByMeterNumber(request.getMeterNumber())) {
            throw new DuplicateResourceException("Meter", "meterNumber", request.getMeterNumber());
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessException("Cannot assign meter to inactive customer");
        }

        Tariff tariff = null;
        if (request.getTariffId() != null) {
            tariff = tariffRepository.findById(request.getTariffId())
                .orElseThrow(() -> new ResourceNotFoundException("Tariff", "id", request.getTariffId()));
        }

        Meter meter = Meter.builder()
            .meterNumber(request.getMeterNumber())
            .meterType(request.getMeterType())
            .installationDate(request.getInstallationDate())
            .status(MeterStatus.ACTIVE)
            .customer(customer)
            .tariff(tariff)
            .location(request.getLocation())
            .initialReading(request.getInitialReading() != null ? BigDecimal.valueOf(request.getInitialReading()) : BigDecimal.ZERO)
            .build();

        meter = meterRepository.save(meter);
        auditService.log(AuditAction.CREATE, "Meter", meter.getId().toString(),
            "Meter created: " + meter.getMeterNumber() + " for customer: " + customer.getFullName());
        return toResponse(meter);
    }

    @Override
    @Transactional(readOnly = true)
    public MeterResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public MeterResponse getByMeterNumber(String meterNumber) {
        Meter m = meterRepository.findByMeterNumber(meterNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Meter", "meterNumber", meterNumber));
        return toResponse(m);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MeterResponse> getAll(Pageable pageable) {
        return PagedResponse.of(meterRepository.findAll(pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MeterResponse> getByCustomer(Long customerId, Pageable pageable) {
        return PagedResponse.of(meterRepository.findByCustomerId(customerId, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MeterResponse> search(String keyword, Pageable pageable) {
        return PagedResponse.of(meterRepository.searchMeters(keyword, pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public MeterResponse update(Long id, UpdateMeterRequest request) {
        Meter meter = findById(id);
        if (request.getStatus() != null) meter.setStatus(request.getStatus());
        if (request.getLocation() != null) meter.setLocation(request.getLocation());
        if (request.getInstallationDate() != null) meter.setInstallationDate(request.getInstallationDate());
        if (request.getTariffId() != null) {
            Tariff tariff = tariffRepository.findById(request.getTariffId())
                .orElseThrow(() -> new ResourceNotFoundException("Tariff", "id", request.getTariffId()));
            meter.setTariff(tariff);
        }
        meter = meterRepository.save(meter);
        auditService.log(AuditAction.UPDATE, "Meter", id.toString(), "Meter updated");
        return toResponse(meter);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Meter meter = findById(id);
        if (!meter.getReadings().isEmpty()) {
            throw new BusinessException("Cannot delete meter with existing readings");
        }
        meterRepository.delete(meter);
        auditService.log(AuditAction.DELETE, "Meter", id.toString(), "Meter deleted");
    }

    @Override
    @Transactional
    public void updateStatus(Long id, MeterStatus status) {
        Meter meter = findById(id);
        meter.setStatus(status);
        meterRepository.save(meter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterResponse> getActiveByCustomer(Long customerId) {
        return meterRepository.findByCustomerIdAndStatus(customerId, MeterStatus.ACTIVE)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private Meter findById(Long id) {
        return meterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Meter", "id", id));
    }

    private MeterResponse toResponse(Meter m) {
        return MeterResponse.builder()
            .id(m.getId())
            .meterNumber(m.getMeterNumber())
            .meterType(m.getMeterType())
            .status(m.getStatus())
            .installationDate(m.getInstallationDate())
            .location(m.getLocation())
            .initialReading(m.getInitialReading())
            .customerId(m.getCustomer().getId())
            .customerName(m.getCustomer().getFullName())
            .tariffId(m.getTariff() != null ? m.getTariff().getId() : null)
            .tariffName(m.getTariff() != null ? m.getTariff().getName() : null)
            .createdAt(m.getCreatedAt())
            .updatedAt(m.getUpdatedAt())
            .build();
    }
}
