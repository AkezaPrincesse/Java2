package com.exam.utility.controller;

import com.exam.utility.dto.request.billing.ApproveBillRequest;
import com.exam.utility.dto.request.billing.GenerateBillsRequest;
import com.exam.utility.dto.request.billing.RejectBillRequest;
import com.exam.utility.dto.response.ApiResponse;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.billing.BillResponse;
import com.exam.utility.enums.BillStatus;
import com.exam.utility.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for bill lifecycle management.
 *
 * Endpoints:
 * - POST /generate  — Batch-generate bills for all active meters in a billing period (ADMIN/FINANCE).
 * - POST /approve   — Approve a PENDING bill (ADMIN/FINANCE). Triggers email notification to customer.
 * - GET  /          — Paginated list of all bills (ADMIN/FINANCE/MANAGER).
 * - GET  /{id}      — Retrieve a bill by ID.
 * - GET  /customer/{customerId} — Bills for a specific customer.
 * - GET  /my-bills  — Bills for the currently logged-in customer.
 * - GET  /number/{billNumber}/pdf — Download bill as PDF.
 * - PATCH /{id}/cancel — Cancel a bill (ADMIN only; PAID bills cannot be cancelled).
 */
@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Billing", description = "Bill generation, approval, and management")
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Generate monthly bills for all active meters")
    public ResponseEntity<ApiResponse<List<BillResponse>>> generateBills(
        @Valid @RequestBody GenerateBillsRequest request
    ) {
        List<BillResponse> bills = billingService.generateBills(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Generated " + bills.size() + " bills", bills));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Approve a pending bill")
    public ResponseEntity<ApiResponse<BillResponse>> approveBill(@Valid @RequestBody ApproveBillRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bill approved", billingService.approveBill(request)));
    }

    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Reject a pending bill")
    public ResponseEntity<ApiResponse<BillResponse>> rejectBill(@Valid @RequestBody RejectBillRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bill rejected", billingService.rejectBill(request)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    @Operation(summary = "Search bills by number, customer, or meter")
    public ResponseEntity<ApiResponse<PagedResponse<BillResponse>>> searchBills(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Search results",
            billingService.searchBills(keyword, PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE', 'MANAGER')")
    @Operation(summary = "Get bill by ID")
    public ResponseEntity<ApiResponse<BillResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved", billingService.getBillById(id)));
    }

    @GetMapping("/number/{billNumber}")
    @Operation(summary = "Get bill by bill number")
    public ResponseEntity<ApiResponse<BillResponse>> getByNumber(@PathVariable String billNumber) {
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved", billingService.getBillByNumber(billNumber)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    @Operation(summary = "Get all bills (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<BillResponse>>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success("Bills retrieved",
            billingService.getAllBills(PageRequest.of(page, size, sort))));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(summary = "Get bills for a specific customer")
    public ResponseEntity<ApiResponse<PagedResponse<BillResponse>>> getByCustomer(
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Bills retrieved",
            billingService.getBillsByCustomer(customerId, PageRequest.of(page, size,
                Sort.by("createdAt").descending()))));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get bills by status")
    public ResponseEntity<ApiResponse<PagedResponse<BillResponse>>> getByStatus(
        @PathVariable BillStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Bills retrieved",
            billingService.getBillsByStatus(status, PageRequest.of(page, size))));
    }

    @GetMapping("/my-bills")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get bills for the currently logged-in customer")
    public ResponseEntity<ApiResponse<PagedResponse<BillResponse>>> getMyBills(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Your bills",
            billingService.getMyBills(PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a bill")
    public ResponseEntity<ApiResponse<Void>> cancelBill(@PathVariable Long id) {
        billingService.cancelBill(id);
        return ResponseEntity.ok(ApiResponse.success("Bill cancelled"));
    }

    @GetMapping("/number/{billNumber}/pdf")
    @Operation(summary = "Download bill as PDF")
    public ResponseEntity<byte[]> downloadBillPdf(@PathVariable String billNumber) {
        byte[] pdf = billingService.generateBillPdf(billNumber);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"bill-" + billNumber + ".pdf\"")
            .body(pdf);
    }
}
