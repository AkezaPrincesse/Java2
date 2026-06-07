package com.exam.utility.controller;

import com.exam.utility.dto.request.payment.CreatePaymentRequest;
import com.exam.utility.dto.response.ApiResponse;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.payment.PaymentResponse;
import com.exam.utility.service.PaymentService;
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

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Payment processing and management")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(summary = "Process a bill payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
        @Valid @RequestBody CreatePaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Payment processed successfully", paymentService.processPayment(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", paymentService.getById(id)));
    }

    @GetMapping("/receipt/{receiptNumber}")
    @Operation(summary = "Get payment by receipt number")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByReceipt(@PathVariable String receiptNumber) {
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved",
            paymentService.getByReceiptNumber(receiptNumber)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get all payments (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved",
            paymentService.getAllPayments(PageRequest.of(page, size, Sort.by("paymentDate").descending()))));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(summary = "Get payments by customer")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getByCustomer(
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved",
            paymentService.getPaymentsByCustomer(customerId, PageRequest.of(page, size))));
    }

    @GetMapping("/bill/{billId}")
    @Operation(summary = "Get payments for a specific bill")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getByBill(
        @PathVariable Long billId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved",
            paymentService.getPaymentsByBill(billId, PageRequest.of(page, size))));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Search payments by receipt, customer, or bill number")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> search(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Search results",
            paymentService.searchPayments(keyword, PageRequest.of(page, size))));
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get payments for the currently logged-in customer")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getMyPayments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Your payments",
            paymentService.getMyPayments(PageRequest.of(page, size, Sort.by("paymentDate").descending()))));
    }

    @GetMapping("/receipt/{receiptNumber}/pdf")
    @Operation(summary = "Download payment receipt as PDF")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String receiptNumber) {
        byte[] pdf = paymentService.generateReceiptPdf(receiptNumber);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"receipt-" + receiptNumber + ".pdf\"")
            .body(pdf);
    }
}
