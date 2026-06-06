package com.exam.utility.service;

import com.exam.utility.dto.request.reading.CreateMeterReadingRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.reading.MeterReadingResponse;
import org.springframework.data.domain.Pageable;

public interface MeterReadingService {
    MeterReadingResponse create(CreateMeterReadingRequest request);
    MeterReadingResponse getById(Long id);
    PagedResponse<MeterReadingResponse> getByMeter(Long meterId, Pageable pageable);
    MeterReadingResponse getLatestByMeter(Long meterId);
    void delete(Long id);
}
