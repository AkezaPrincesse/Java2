package com.exam.utility.service.impl;

import com.exam.utility.billing.BillingEngine;
import com.exam.utility.dto.request.billing.ApproveBillRequest;
import com.exam.utility.dto.request.billing.GenerateBillsRequest;
import com.exam.utility.dto.request.billing.RejectBillRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.billing.BillItemResponse;
import com.exam.utility.dto.response.billing.BillResponse;
import com.exam.utility.entity.Bill;
import com.exam.utility.entity.Customer;
import com.exam.utility.entity.User;
import com.exam.utility.enums.AuditAction;
import com.exam.utility.enums.BillStatus;
import com.exam.utility.enums.MeterType;
import com.exam.utility.exception.BusinessException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.BillRepository;
import com.exam.utility.repository.CustomerRepository;
import com.exam.utility.repository.UserRepository;
import com.exam.utility.service.AuditService;
import com.exam.utility.service.BillingService;
import com.exam.utility.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates the bill lifecycle: generation, approval, customer lookups, and PDF export.
 *
 * Business rules enforced here:
 * - Only PENDING bills can be approved.
 * - Only the authenticated user's email is used to resolve "my bills" (prevents cross-customer access).
 * - PAID bills cannot be cancelled.
 * - Bills with any payments cannot be cancelled.
 * - Audit log entries are written for generation and approval events.
 * - Customers are notified by email/notification on each state transition.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements BillingService {

    private final BillingEngine billingEngine;
    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Override
    @Transactional
    public List<BillResponse> generateBills(GenerateBillsRequest request) {
        List<Bill> bills = billingEngine.generateMonthlyBills(request.getBillingYear(), request.getBillingMonth());
        bills.forEach(notificationService::notifyBillGenerated);
        auditService.log(AuditAction.BILL_GENERATED, "Bill", null,
            "Generated " + bills.size() + " bills for " + request.getBillingYear() + "/" + request.getBillingMonth());
        return bills.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BillResponse approveBill(ApproveBillRequest request) {
        Bill bill = billRepository.findByBillNumber(request.getBillNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Bill", "billNumber", request.getBillNumber()));

        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BusinessException("Only PENDING bills can be approved. Current status: " + bill.getStatus());
        }

        String approver = SecurityContextHolder.getContext().getAuthentication().getName();
        bill.setStatus(BillStatus.APPROVED);
        bill.setApprovedAt(LocalDateTime.now());
        bill.setApprovedBy(approver);
        if (request.getNotes() != null) bill.setNotes(request.getNotes());

        bill = billRepository.save(bill);
        notificationService.notifyBillApproved(bill);
        auditService.log(AuditAction.BILL_APPROVED, "Bill", bill.getId().toString(),
            "Bill approved: " + bill.getBillNumber());

        return toResponse(bill);
    }

    @Override
    @Transactional
    public BillResponse rejectBill(RejectBillRequest request) {
        Bill bill = billRepository.findByBillNumber(request.getBillNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Bill", "billNumber", request.getBillNumber()));

        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BusinessException("Only PENDING bills can be rejected. Current status: " + bill.getStatus());
        }

        bill.setStatus(BillStatus.CANCELLED);
        bill.setNotes("REJECTED: " + request.getReason());
        bill = billRepository.save(bill);

        auditService.log(AuditAction.UPDATE, "Bill", bill.getId().toString(),
            "Bill rejected: " + bill.getBillNumber() + " — " + request.getReason());

        return toResponse(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BillResponse> searchBills(String keyword, Pageable pageable) {
        return PagedResponse.of(billRepository.searchBills(keyword, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BillResponse getBillByNumber(String billNumber) {
        return toResponse(billRepository.findByBillNumber(billNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Bill", "billNumber", billNumber)));
    }

    @Override
    @Transactional(readOnly = true)
    public BillResponse getBillById(Long id) {
        return toResponse(billRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bill", "id", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BillResponse> getAllBills(Pageable pageable) {
        return PagedResponse.of(billRepository.findAll(pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BillResponse> getBillsByCustomer(Long customerId, Pageable pageable) {
        return PagedResponse.of(billRepository.findByCustomerId(customerId, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BillResponse> getBillsByStatus(BillStatus status, Pageable pageable) {
        return PagedResponse.of(billRepository.findByStatus(status, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BillResponse> getMyBills(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found for logged-in user"));
        return getBillsByCustomer(customer.getId(), pageable);
    }

    @Override
    @Transactional
    public void cancelBill(Long id) {
        Bill bill = billRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bill", "id", id));

        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessException("Cannot cancel a paid bill");
        }
        if (!bill.getPayments().isEmpty()) {
            throw new BusinessException("Cannot cancel a bill with payments");
        }

        bill.setStatus(BillStatus.CANCELLED);
        billRepository.save(bill);
        auditService.log(AuditAction.UPDATE, "Bill", id.toString(), "Bill cancelled");
    }

    @Override
    public byte[] generateBillPdf(String billNumber) {
        Bill bill = billRepository.findByBillNumber(billNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Bill", "billNumber", billNumber));

        // PDF generation using OpenPDF
        try {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 10);

            document.add(new com.lowagie.text.Paragraph("UTILITY BILLING SYSTEM", titleFont));
            document.add(new com.lowagie.text.Paragraph(
                bill.getUtilityType() == MeterType.WATER ? "WASAC – Water Bill" : "REG – Electricity Bill", headerFont));
            document.add(com.lowagie.text.Chunk.NEWLINE);
            document.add(new com.lowagie.text.Paragraph("Bill Number: " + bill.getBillNumber(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Customer: " + bill.getCustomer().getFullName(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Meter: " + bill.getMeter().getMeterNumber(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Bill Date: " + bill.getBillDate(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Due Date: " + bill.getDueDate(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Status: " + bill.getStatus(), normalFont));
            document.add(com.lowagie.text.Chunk.NEWLINE);
            document.add(new com.lowagie.text.Paragraph("CHARGES BREAKDOWN", headerFont));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(2);
            table.setWidthPercentage(100);
            addTableRow(table, "Consumption Charge", bill.getConsumptionAmount() + " RWF");
            addTableRow(table, "Service Charges", bill.getServiceChargeAmount() + " RWF");
            addTableRow(table, "Tax", bill.getTaxAmount() + " RWF");
            addTableRow(table, "Penalty", bill.getPenaltyAmount() + " RWF");
            addTableRow(table, "TOTAL", bill.getTotalAmount() + " RWF");
            addTableRow(table, "Paid", bill.getPaidAmount() + " RWF");
            addTableRow(table, "BALANCE DUE", bill.getBalanceAmount() + " RWF");
            document.add(table);

            document.add(com.lowagie.text.Chunk.NEWLINE);
            document.add(new com.lowagie.text.Paragraph(
                "Thank you for using WASAC/REG Utility Services. Please pay before " + bill.getDueDate() + ".", normalFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for bill {}: {}", billNumber, e.getMessage());
            throw new BusinessException("Failed to generate bill PDF");
        }
    }

    private void addTableRow(com.lowagie.text.pdf.PdfPTable table, String key, String value) {
        table.addCell(key);
        table.addCell(value);
    }

    private BillResponse toResponse(Bill b) {
        List<BillItemResponse> items = b.getItems().stream()
            .map(i -> BillItemResponse.builder()
                .id(i.getId())
                .itemType(i.getItemType())
                .description(i.getDescription())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .amount(i.getAmount())
                .build())
            .collect(Collectors.toList());

        return BillResponse.builder()
            .id(b.getId())
            .billNumber(b.getBillNumber())
            .customerId(b.getCustomer().getId())
            .customerName(b.getCustomer().getFullName())
            .customerEmail(b.getCustomer().getEmail())
            .meterId(b.getMeter().getId())
            .meterNumber(b.getMeter().getMeterNumber())
            .utilityType(b.getUtilityType())
            .billingYear(b.getBillingCycle().getBillingYear())
            .billingMonth(b.getBillingCycle().getBillingMonth())
            .consumption(b.getMeterReading() != null ? b.getMeterReading().getConsumption().doubleValue() : 0.0)
            .consumptionAmount(b.getConsumptionAmount())
            .serviceChargeAmount(b.getServiceChargeAmount())
            .taxAmount(b.getTaxAmount())
            .penaltyAmount(b.getPenaltyAmount())
            .totalAmount(b.getTotalAmount())
            .paidAmount(b.getPaidAmount())
            .balanceAmount(b.getBalanceAmount())
            .status(b.getStatus())
            .billDate(b.getBillDate())
            .dueDate(b.getDueDate())
            .approvedAt(b.getApprovedAt())
            .approvedBy(b.getApprovedBy())
            .paidAt(b.getPaidAt())
            .notes(b.getNotes())
            .items(items)
            .createdAt(b.getCreatedAt())
            .build();
    }
}
