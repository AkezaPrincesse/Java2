package com.exam.utility.service.impl;

import com.exam.utility.dto.response.dashboard.DashboardStatsResponse;
import com.exam.utility.enums.BillStatus;
import com.exam.utility.enums.CustomerStatus;
import com.exam.utility.enums.MeterStatus;
import com.exam.utility.enums.MeterType;
import com.exam.utility.repository.*;
import com.exam.utility.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final CustomerRepository customerRepository;
    private final MeterRepository meterRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        Map<String, BigDecimal> paymentsByMethod = new HashMap<>();
        List<Object[]> methodSums = paymentRepository.sumByPaymentMethod();
        for (Object[] row : methodSums) {
            paymentsByMethod.put(row[0].toString(), (BigDecimal) row[1]);
        }

        BigDecimal totalRevenue = billRepository.sumTotalBilled();
        BigDecimal collectedRevenue = billRepository.sumTotalPaid();
        BigDecimal outstanding = billRepository.sumOutstandingBalance();

        return DashboardStatsResponse.builder()
            .totalCustomers(customerRepository.count())
            .activeCustomers(customerRepository.countByStatus(CustomerStatus.ACTIVE))
            .totalMeters(meterRepository.count())
            .activeMeters(meterRepository.countByStatus(MeterStatus.ACTIVE))
            .waterMeters(meterRepository.countByMeterType(MeterType.WATER))
            .electricityMeters(meterRepository.countByMeterType(MeterType.ELECTRICITY))
            .totalBills(billRepository.count())
            .pendingBills(billRepository.countByStatus(BillStatus.PENDING))
            .paidBills(billRepository.countByStatus(BillStatus.PAID))
            .overdueBills(billRepository.countByStatus(BillStatus.OVERDUE))
            .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
            .collectedRevenue(collectedRevenue != null ? collectedRevenue : BigDecimal.ZERO)
            .outstandingBalance(outstanding != null ? outstanding : BigDecimal.ZERO)
            .paymentsByMethod(paymentsByMethod)
            .build();
    }

    @Override
    public byte[] generateMonthlyReport(int year, int month) {
        try {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 10);

            document.add(new com.lowagie.text.Paragraph("MONTHLY BILLING REPORT", titleFont));
            document.add(new com.lowagie.text.Paragraph("Period: " + year + "/" + String.format("%02d", month), normalFont));
            document.add(new com.lowagie.text.Paragraph("Generated: " + LocalDateTime.now(), normalFont));
            document.add(com.lowagie.text.Chunk.NEWLINE);

            DashboardStatsResponse stats = getDashboardStats();
            document.add(new com.lowagie.text.Paragraph("SUMMARY", new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD)));
            document.add(new com.lowagie.text.Paragraph("Total Customers: " + stats.getTotalCustomers(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Active Meters: " + stats.getActiveMeters(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Total Bills: " + stats.getTotalBills(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Total Revenue: " + stats.getTotalRevenue() + " RWF", normalFont));
            document.add(new com.lowagie.text.Paragraph("Collected: " + stats.getCollectedRevenue() + " RWF", normalFont));
            document.add(new com.lowagie.text.Paragraph("Outstanding: " + stats.getOutstandingBalance() + " RWF", normalFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate monthly report: {}", e.getMessage());
            throw new com.exam.utility.exception.BusinessException("Failed to generate monthly report");
        }
    }

    @Override
    public byte[] generateRevenueReport(LocalDate from, LocalDate to) {
        try {
            BigDecimal revenue = paymentRepository.sumPaymentsBetween(
                from.atStartOfDay(), to.atTime(23, 59, 59));

            com.lowagie.text.Document document = new com.lowagie.text.Document();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
            document.add(new com.lowagie.text.Paragraph("REVENUE REPORT", titleFont));
            document.add(new com.lowagie.text.Paragraph("Period: " + from + " to " + to));
            document.add(new com.lowagie.text.Paragraph("Total Revenue Collected: " +
                (revenue != null ? revenue : BigDecimal.ZERO) + " RWF"));
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate revenue report: {}", e.getMessage());
            throw new com.exam.utility.exception.BusinessException("Failed to generate revenue report");
        }
    }
}
