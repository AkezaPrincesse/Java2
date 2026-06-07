package com.exam.utility.service;

import com.exam.utility.dto.request.payment.CreatePaymentRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.payment.PaymentResponse;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentResponse processPayment(CreatePaymentRequest request);
    PaymentResponse getByReceiptNumber(String receiptNumber);
    PaymentResponse getById(Long id);
    PagedResponse<PaymentResponse> getAllPayments(Pageable pageable);
    PagedResponse<PaymentResponse> getPaymentsByCustomer(Long customerId, Pageable pageable);
    PagedResponse<PaymentResponse> getPaymentsByBill(Long billId, Pageable pageable);
    PagedResponse<PaymentResponse> searchPayments(String keyword, Pageable pageable);
    PagedResponse<PaymentResponse> getMyPayments(Pageable pageable);
    byte[] generateReceiptPdf(String receiptNumber);
}
