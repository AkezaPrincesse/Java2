package com.exam.utility.service.impl;

import com.exam.utility.dto.request.payment.CreatePaymentRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.payment.PaymentResponse;
import com.exam.utility.entity.Bill;
import com.exam.utility.entity.Payment;
import com.exam.utility.enums.AuditAction;
import com.exam.utility.enums.BillStatus;
import com.exam.utility.enums.PaymentStatus;
import com.exam.utility.exception.BusinessException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.BillRepository;
import com.exam.utility.repository.PaymentRepository;
import com.exam.utility.service.AuditService;
import com.exam.utility.service.NotificationService;
import com.exam.utility.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Override
    @Transactional
    public PaymentResponse processPayment(CreatePaymentRequest request) {
        Bill bill = billRepository.findByBillNumber(request.getBillNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Bill", "billNumber", request.getBillNumber()));

        if (bill.getStatus() == BillStatus.CANCELLED) {
            throw new BusinessException("Cannot make payment on a cancelled bill");
        }
        if (bill.getStatus() == BillStatus.PENDING) {
            throw new BusinessException("Bill must be approved before payment can be made");
        }
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessException("Bill is already fully paid");
        }

        if (request.getAmount().compareTo(bill.getBalanceAmount()) > 0) {
            throw new BusinessException(
                "Payment amount " + request.getAmount() + " exceeds balance " + bill.getBalanceAmount());
        }

        String receiptNumber = generateReceiptNumber();

        Payment payment = Payment.builder()
            .receiptNumber(receiptNumber)
            .bill(bill)
            .customer(bill.getCustomer())
            .amount(request.getAmount())
            .paymentMethod(request.getPaymentMethod())
            .paymentStatus(PaymentStatus.COMPLETED)
            .paymentDate(LocalDateTime.now())
            .transactionReference(request.getTransactionReference())
            .paidBy(request.getPaidBy())
            .notes(request.getNotes())
            .build();

        payment = paymentRepository.save(payment);

        // Update bill balance
        BigDecimal newPaid = bill.getPaidAmount().add(request.getAmount());
        BigDecimal newBalance = bill.getTotalAmount().subtract(newPaid);

        bill.setPaidAmount(newPaid);
        bill.setBalanceAmount(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            bill.setStatus(BillStatus.PAID);
            bill.setPaidAt(LocalDateTime.now());
        } else {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        }

        billRepository.save(bill);

        notificationService.notifyPaymentReceived(payment);
        auditService.log(AuditAction.PAYMENT_PROCESSED, "Payment", payment.getId().toString(),
            "Payment processed: " + receiptNumber + " amount: " + request.getAmount());

        log.info("Payment processed: {} for bill: {} amount: {}", receiptNumber, request.getBillNumber(), request.getAmount());
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByReceiptNumber(String receiptNumber) {
        return toResponse(paymentRepository.findByReceiptNumber(receiptNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "receiptNumber", receiptNumber)));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return toResponse(paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getAllPayments(Pageable pageable) {
        return PagedResponse.of(paymentRepository.findAll(pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getPaymentsByCustomer(Long customerId, Pageable pageable) {
        return PagedResponse.of(paymentRepository.findByCustomerId(customerId, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getPaymentsByBill(Long billId, Pageable pageable) {
        return PagedResponse.of(paymentRepository.findByBillId(billId, pageable).map(this::toResponse));
    }

    @Override
    public byte[] generateReceiptPdf(String receiptNumber) {
        Payment payment = paymentRepository.findByReceiptNumber(receiptNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "receiptNumber", receiptNumber));

        try {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 10);

            document.add(new com.lowagie.text.Paragraph("PAYMENT RECEIPT", titleFont));
            document.add(new com.lowagie.text.Paragraph("WASAC / REG Utility Billing System", normalFont));
            document.add(com.lowagie.text.Chunk.NEWLINE);
            document.add(new com.lowagie.text.Paragraph("Receipt No: " + payment.getReceiptNumber(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Bill No: " + payment.getBill().getBillNumber(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Customer: " + payment.getCustomer().getFullName(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Amount Paid: " + payment.getAmount() + " RWF", normalFont));
            document.add(new com.lowagie.text.Paragraph("Payment Method: " + payment.getPaymentMethod(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Payment Date: " + payment.getPaymentDate(), normalFont));
            if (payment.getTransactionReference() != null) {
                document.add(new com.lowagie.text.Paragraph("Transaction Ref: " + payment.getTransactionReference(), normalFont));
            }
            document.add(com.lowagie.text.Chunk.NEWLINE);
            document.add(new com.lowagie.text.Paragraph("Thank you for your payment!", normalFont));
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate receipt PDF for {}: {}", receiptNumber, e.getMessage());
            throw new BusinessException("Failed to generate receipt PDF");
        }
    }

    private String generateReceiptNumber() {
        return "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
            .id(p.getId())
            .receiptNumber(p.getReceiptNumber())
            .billNumber(p.getBill().getBillNumber())
            .customerId(p.getCustomer().getId())
            .customerName(p.getCustomer().getFullName())
            .amount(p.getAmount())
            .paymentMethod(p.getPaymentMethod())
            .paymentStatus(p.getPaymentStatus())
            .paymentDate(p.getPaymentDate())
            .transactionReference(p.getTransactionReference())
            .paidBy(p.getPaidBy())
            .notes(p.getNotes())
            .createdAt(p.getCreatedAt())
            .build();
    }
}
