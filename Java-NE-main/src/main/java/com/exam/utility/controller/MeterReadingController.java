package com.exam.utility.controller;

import com.exam.utility.dto.request.reading.CreateMeterReadingRequest;
import com.exam.utility.dto.response.ApiResponse;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.reading.MeterReadingResponse;
import com.exam.utility.service.MeterReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meter-readings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Meter Readings", description = "Meter reading capture and management")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Record a new meter reading")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> create(
        @Valid @RequestBody CreateMeterReadingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Meter reading recorded", meterReadingService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get meter reading by ID")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Reading retrieved", meterReadingService.getById(id)));
    }

    @GetMapping("/meter/{meterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get readings for a specific meter")
    public ResponseEntity<ApiResponse<PagedResponse<MeterReadingResponse>>> getByMeter(
        @PathVariable Long meterId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Readings retrieved",
            meterReadingService.getByMeter(meterId, PageRequest.of(page, size, Sort.by("readingDate").descending()))));
    }

    @GetMapping("/meter/{meterId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get latest reading for a meter")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> getLatest(@PathVariable Long meterId) {
        return ResponseEntity.ok(ApiResponse.success("Latest reading retrieved",
            meterReadingService.getLatestByMeter(meterId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a meter reading")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        meterReadingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Meter reading deleted"));
    }
}
