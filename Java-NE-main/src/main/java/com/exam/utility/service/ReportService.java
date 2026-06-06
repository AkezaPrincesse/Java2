package com.exam.utility.service;

import com.exam.utility.dto.response.dashboard.DashboardStatsResponse;

import java.time.LocalDate;

public interface ReportService {
    DashboardStatsResponse getDashboardStats();
    byte[] generateMonthlyReport(int year, int month);
    byte[] generateRevenueReport(LocalDate from, LocalDate to);
}
