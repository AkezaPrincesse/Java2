package com.exam.utility.dto.response.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalCustomers;
    private long activeCustomers;
    private long totalMeters;
    private long activeMeters;
    private long waterMeters;
    private long electricityMeters;
    private long totalBills;
    private long pendingBills;
    private long paidBills;
    private long overdueBills;
    private BigDecimal totalRevenue;
    private BigDecimal collectedRevenue;
    private BigDecimal outstandingBalance;
    private Map<String, BigDecimal> revenueByMonth;
    private Map<String, Long> billsByStatus;
    private Map<String, BigDecimal> paymentsByMethod;
}
