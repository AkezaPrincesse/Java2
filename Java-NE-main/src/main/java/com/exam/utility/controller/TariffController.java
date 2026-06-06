package com.exam.utility.controller;

import com.exam.utility.dto.request.tariff.CreateTariffRequest;
import com.exam.utility.dto.response.ApiResponse;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.tariff.TariffResponse;
import com.exam.utility.enums.MeterType;
import com.exam.utility.service.TariffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tariffs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tariffs", description = "Tariff and pricing management")
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new tariff")
    public ResponseEntity<ApiResponse<TariffResponse>> create(@Valid @RequestBody CreateTariffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tariff created", tariffService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tariff by ID")
    public ResponseEntity<ApiResponse<TariffResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tariff retrieved", tariffService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Get all tariffs (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<TariffResponse>>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Tariffs retrieved",
            tariffService.getAll(PageRequest.of(page, size))));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active tariffs by utility type")
    public ResponseEntity<ApiResponse<List<TariffResponse>>> getActive(@RequestParam MeterType utilityType) {
        return ResponseEntity.ok(ApiResponse.success("Active tariffs retrieved",
            tariffService.getActiveByUtilityType(utilityType)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tariff")
    public ResponseEntity<ApiResponse<TariffResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody CreateTariffRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Tariff updated", tariffService.update(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a tariff")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        tariffService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Tariff deactivated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a tariff")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tariffService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Tariff deleted"));
    }
}
