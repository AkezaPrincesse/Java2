package com.exam.utility.scheduler;

import com.exam.utility.billing.BillingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingScheduler {

    private final BillingEngine billingEngine;

    @Scheduled(cron = "${app.billing.cron-monthly}")
    public void generateMonthlyBills() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue() - 1;
        if (month == 0) { month = 12; year--; }

        log.info("Scheduled job: generating bills for {}/{}", year, month);
        try {
            billingEngine.generateMonthlyBills(year, month);
        } catch (Exception e) {
            log.error("Scheduled bill generation failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${app.billing.cron-penalty}")
    public void applyPenalties() {
        log.info("Scheduled job: applying penalties to overdue bills");
        try {
            billingEngine.applyPenaltiesToOverdueBills();
        } catch (Exception e) {
            log.error("Scheduled penalty application failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        log.info("Scheduled job: cleaning up expired tokens");
        // Token cleanup is handled at repository level
    }
}
