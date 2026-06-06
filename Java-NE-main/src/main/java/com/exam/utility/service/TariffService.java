package com.exam.utility.service;

import com.exam.utility.dto.request.tariff.CreateTariffRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.tariff.TariffResponse;
import com.exam.utility.enums.MeterType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TariffService {
    TariffResponse create(CreateTariffRequest request);
    TariffResponse getById(Long id);
    PagedResponse<TariffResponse> getAll(Pageable pageable);
    List<TariffResponse> getActiveByUtilityType(MeterType utilityType);
    TariffResponse update(Long id, CreateTariffRequest request);
    void deactivate(Long id);
    void delete(Long id);
}
