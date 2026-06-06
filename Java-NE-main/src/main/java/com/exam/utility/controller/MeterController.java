package com.exam.utility.controller;

import com.exam.utility.dto.request.meter.CreateMeterRequest;
import com.exam.utility.dto.request.meter.UpdateMeterRequest;
import com.exam.utility.dto.response.ApiResponse;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.meter.MeterResponse;
import com.exam.utility.enums.MeterStatus;
import com.exam.utility.service.MeterService;
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

import java.util.List;

@RestController
@RequestMapping("/meters")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Meters", description = "Utility meter management")
public class MeterController {

    private final MeterService meterService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Install a new meter")
    public ResponseEntity<ApiResponse<MeterResponse>> create(@Valid @RequestBody CreateMeterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Meter created", meterService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get meter by ID")
    public ResponseEntity<ApiResponse<MeterResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Meter retrieved", meterService.getById(id)));
    }

    @GetMapping("/number/{meterNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Get meter by meter number")
    public ResponseEntity<ApiResponse<MeterResponse>> getByNumber(@PathVariable String meterNumber) {
        return ResponseEntity.ok(ApiResponse.success("Meter retrieved", meterService.getByMeterNumber(meterNumber)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE', 'MANAGER')")
    @Operation(summary = "Get all meters (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<MeterResponse>>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success("Meters retrieved",
            meterService.getAll(PageRequest.of(page, size, sort))));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get meters belonging to a customer")
    public ResponseEntity<ApiResponse<PagedResponse<MeterResponse>>> getByCustomer(
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Meters retrieved",
            meterService.getByCustomer(customerId, PageRequest.of(page, size))));
    }

    @GetMapping("/customer/{customerId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get active meters for a customer")
    public ResponseEntity<ApiResponse<List<MeterResponse>>> getActiveByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("Active meters retrieved",
            meterService.getActiveByCustomer(customerId)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Search meters by keyword")
    public ResponseEntity<ApiResponse<PagedResponse<MeterResponse>>> search(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Search results",
            meterService.search(keyword, PageRequest.of(page, size))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Update meter details")
    public ResponseEntity<ApiResponse<MeterResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateMeterRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Meter updated", meterService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update meter status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long id, @RequestParam MeterStatus status) {
        meterService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Meter status updated to: " + status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a meter")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        meterService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Meter deleted successfully"));
    }
}
