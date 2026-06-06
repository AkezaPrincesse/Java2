package com.exam.utility.service.impl;

import com.exam.utility.dto.request.tariff.CreateTariffRequest;
import com.exam.utility.dto.request.tariff.TariffTierRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.tariff.TariffResponse;
import com.exam.utility.dto.response.tariff.TariffVersionResponse;
import com.exam.utility.entity.Tariff;
import com.exam.utility.entity.TariffVersion;
import com.exam.utility.enums.AuditAction;
import com.exam.utility.enums.MeterType;
import com.exam.utility.enums.TariffType;
import com.exam.utility.exception.BusinessException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.TariffRepository;
import com.exam.utility.repository.TariffVersionRepository;
import com.exam.utility.service.AuditService;
import com.exam.utility.service.TariffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TariffServiceImpl implements TariffService {

    private final TariffRepository tariffRepository;
    private final TariffVersionRepository tariffVersionRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public TariffResponse create(CreateTariffRequest request) {
        if (request.getTariffType() == TariffType.FLAT && request.getFlatRate() == null) {
            throw new BusinessException("Flat rate is required for FLAT tariff type");
        }
        if (request.getTariffType() == TariffType.TIERED &&
            (request.getTiers() == null || request.getTiers().isEmpty())) {
            throw new BusinessException("Tiers are required for TIERED tariff type");
        }

        Tariff tariff = Tariff.builder()
            .name(request.getName())
            .description(request.getDescription())
            .utilityType(request.getUtilityType())
            .tariffType(request.getTariffType())
            .flatRate(request.getFlatRate())
            .effectiveDate(request.getEffectiveDate())
            .expiryDate(request.getExpiryDate())
            .active(true)
            .build();

        tariff = tariffRepository.save(tariff);

        if (request.getTiers() != null) {
            List<TariffVersion> tiers = new ArrayList<>();
            for (TariffTierRequest tierReq : request.getTiers()) {
                tiers.add(TariffVersion.builder()
                    .tariff(tariff)
                    .tierOrder(tierReq.getTierOrder())
                    .minUnits(tierReq.getMinUnits())
                    .maxUnits(tierReq.getMaxUnits())
                    .ratePerUnit(tierReq.getRatePerUnit())
                    .effectiveDate(tierReq.getEffectiveDate())
                    .description(tierReq.getDescription())
                    .build());
            }
            tariffVersionRepository.saveAll(tiers);
        }

        auditService.log(AuditAction.CREATE, "Tariff", tariff.getId().toString(),
            "Tariff created: " + tariff.getName());
        return toResponse(tariff);
    }

    @Override
    @Transactional(readOnly = true)
    public TariffResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TariffResponse> getAll(Pageable pageable) {
        return PagedResponse.of(tariffRepository.findAll(pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TariffResponse> getActiveByUtilityType(MeterType utilityType) {
        return tariffRepository.findByUtilityTypeAndActiveTrue(utilityType)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TariffResponse update(Long id, CreateTariffRequest request) {
        Tariff tariff = findById(id);
        if (request.getName() != null) tariff.setName(request.getName());
        if (request.getDescription() != null) tariff.setDescription(request.getDescription());
        if (request.getFlatRate() != null) tariff.setFlatRate(request.getFlatRate());
        if (request.getEffectiveDate() != null) tariff.setEffectiveDate(request.getEffectiveDate());
        if (request.getExpiryDate() != null) tariff.setExpiryDate(request.getExpiryDate());
        tariff = tariffRepository.save(tariff);
        auditService.log(AuditAction.UPDATE, "Tariff", id.toString(), "Tariff updated");
        return toResponse(tariff);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Tariff tariff = findById(id);
        tariff.setActive(false);
        tariffRepository.save(tariff);
        auditService.log(AuditAction.UPDATE, "Tariff", id.toString(), "Tariff deactivated");
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Tariff tariff = findById(id);
        if (!tariff.getMeters().isEmpty()) {
            throw new BusinessException("Cannot delete tariff assigned to active meters");
        }
        tariffRepository.delete(tariff);
        auditService.log(AuditAction.DELETE, "Tariff", id.toString(), "Tariff deleted");
    }

    private Tariff findById(Long id) {
        return tariffRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tariff", "id", id));
    }

    private TariffResponse toResponse(Tariff t) {
        List<TariffVersionResponse> versions = tariffVersionRepository
            .findByTariffIdOrderByTierOrder(t.getId())
            .stream()
            .map(v -> TariffVersionResponse.builder()
                .id(v.getId())
                .tierOrder(v.getTierOrder())
                .minUnits(v.getMinUnits())
                .maxUnits(v.getMaxUnits())
                .ratePerUnit(v.getRatePerUnit())
                .effectiveDate(v.getEffectiveDate())
                .expiryDate(v.getExpiryDate())
                .description(v.getDescription())
                .build())
            .collect(Collectors.toList());

        return TariffResponse.builder()
            .id(t.getId())
            .name(t.getName())
            .description(t.getDescription())
            .utilityType(t.getUtilityType())
            .tariffType(t.getTariffType())
            .flatRate(t.getFlatRate())
            .effectiveDate(t.getEffectiveDate())
            .expiryDate(t.getExpiryDate())
            .active(t.isActive())
            .versions(versions)
            .createdAt(t.getCreatedAt())
            .updatedAt(t.getUpdatedAt())
            .build();
    }
}
