package com.exam.utility.controller;

import com.exam.utility.dto.request.customer.CreateCustomerRequest;
import com.exam.utility.dto.request.customer.UpdateCustomerRequest;
import com.exam.utility.dto.response.ApiResponse;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.customer.CustomerResponse;
import com.exam.utility.enums.CustomerStatus;
import com.exam.utility.service.CustomerService;
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
@RequestMapping("/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customers", description = "Customer management")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Create a new customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
        @Valid @RequestBody CreateCustomerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Customer created successfully", customerService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved", customerService.getById(id)));
    }

    @GetMapping("/national-id/{nationalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Get customer by National ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getByNationalId(@PathVariable String nationalId) {
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved", customerService.getByNationalId(nationalId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE', 'MANAGER')")
    @Operation(summary = "Get all customers (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerResponse>>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved",
            customerService.getAll(PageRequest.of(page, size, sort))));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Search customers by keyword")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerResponse>>> search(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Search results",
            customerService.search(keyword, PageRequest.of(page, size))));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Get customers by status")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerResponse>>> getByStatus(
        @PathVariable CustomerStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved",
            customerService.getByStatus(status, PageRequest.of(page, size))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Update customer details")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCustomerRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated", customerService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update customer status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
        @PathVariable Long id,
        @RequestParam CustomerStatus status
    ) {
        customerService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Customer status updated to: " + status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a customer")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully"));
    }
}
