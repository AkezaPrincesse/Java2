package com.exam.utility.controller;

import com.exam.utility.dto.response.ApiResponse;
import com.exam.utility.dto.response.dashboard.DashboardStatsResponse;
import com.exam.utility.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports & Analytics", description = "Dashboard statistics and PDF reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics", reportService.getDashboardStats()));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'MANAGER')")
    @Operation(summary = "Download monthly billing report as PDF")
    public ResponseEntity<byte[]> getMonthlyReport(
        @RequestParam int year,
        @RequestParam int month
    ) {
        byte[] pdf = reportService.generateMonthlyReport(year, month);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"report-" + year + "-" + String.format("%02d", month) + ".pdf\"")
            .body(pdf);
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Download revenue report for date range as PDF")
    public ResponseEntity<byte[]> getRevenueReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        byte[] pdf = reportService.generateRevenueReport(from, to);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"revenue-" + from + "-to-" + to + ".pdf\"")
            .body(pdf);
    }
}
