package com.exam.utility.service;

import com.exam.utility.dto.request.meter.CreateMeterRequest;
import com.exam.utility.dto.request.meter.UpdateMeterRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.meter.MeterResponse;
import com.exam.utility.enums.MeterStatus;
import com.exam.utility.enums.MeterType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MeterService {
    MeterResponse create(CreateMeterRequest request);
    MeterResponse getById(Long id);
    MeterResponse getByMeterNumber(String meterNumber);
    PagedResponse<MeterResponse> getAll(Pageable pageable);
    PagedResponse<MeterResponse> getByCustomer(Long customerId, Pageable pageable);
    PagedResponse<MeterResponse> search(String keyword, Pageable pageable);
    MeterResponse update(Long id, UpdateMeterRequest request);
    void delete(Long id);
    void updateStatus(Long id, MeterStatus status);
    List<MeterResponse> getActiveByCustomer(Long customerId);
}
