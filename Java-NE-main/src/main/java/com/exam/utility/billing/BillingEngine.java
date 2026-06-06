package com.exam.utility.billing;

import com.exam.utility.entity.*;
import com.exam.utility.enums.*;
import com.exam.utility.exception.BusinessException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core billing calculation engine.
 *
 * Business rules enforced:
 * - Billing periods in the future are rejected.
 * - Bills cannot be generated without a valid meter reading for the period.
 * - Bills for meters that already have an APPROVED or PAID bill in the cycle are skipped.
 * - Inactive customers are skipped; their history remains intact.
 * - Closed billing cycles cannot have new bills added.
 * - Consumption charges support both flat-rate and tiered tariff structures.
 * - Service charges and taxes are applied on top of consumption; penalties are applied separately.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillingEngine {

    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final MeterRepository meterRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final TariffRepository tariffRepository;
    private final TariffVersionRepository tariffVersionRepository;
    private final ServiceChargeRepository serviceChargeRepository;
    private final TaxRepository taxRepository;
    private final PenaltyRepository penaltyRepository;

    @Transactional
    public List<Bill> generateMonthlyBills(int year, int month) {
        log.info("Starting bill generation for {}/{}", year, month);

        // Billing periods in the future are not allowed — readings and consumption data do not exist yet
        YearMonth requestedPeriod = YearMonth.of(year, month);
        if (requestedPeriod.isAfter(YearMonth.now())) {
            throw new BusinessException(
                "Cannot generate bills for a billing period that has not yet started.");
        }

        BillingCycle cycle = getOrCreateBillingCycle(year, month);
        if (cycle.isClosed()) {
            throw new BusinessException("Billing cycle for " + year + "/" + month + " is already closed");
        }

        List<Meter> activeMeters = meterRepository.findByStatusAndMeterType(MeterStatus.ACTIVE, MeterType.WATER);
        activeMeters.addAll(meterRepository.findByStatusAndMeterType(MeterStatus.ACTIVE, MeterType.ELECTRICITY));

        List<Bill> bills = new ArrayList<>();
        for (Meter meter : activeMeters) {
            if (meter.getCustomer().getStatus() != CustomerStatus.ACTIVE) {
                log.debug("Skipping inactive customer: {}", meter.getCustomer().getFullName());
                continue;
            }
            if (billRepository.existsByMeterIdAndBillingCycleId(meter.getId(), cycle.getId())) {
                // Bills that are APPROVED or PAID must never be regenerated — audit integrity
                boolean hasApprovedBill = billRepository
                    .findByMeterIdAndBillingCycleId(meter.getId(), cycle.getId())
                    .stream()
                    .anyMatch(b -> b.getStatus() == com.exam.utility.enums.BillStatus.APPROVED
                               || b.getStatus() == com.exam.utility.enums.BillStatus.PAID);
                if (hasApprovedBill) {
                    log.warn("Skipping meter {} — approved/paid bill already exists for {}/{}",
                        meter.getMeterNumber(), year, month);
                } else {
                    log.debug("Bill already exists for meter {} in cycle {}/{}", meter.getMeterNumber(), year, month);
                }
                continue;
            }

            try {
                Bill bill = generateBillForMeter(meter, cycle);
                bills.add(bill);
            } catch (Exception e) {
                log.error("Failed to generate bill for meter {}: {}", meter.getMeterNumber(), e.getMessage());
            }
        }

        log.info("Generated {} bills for {}/{}", bills.size(), year, month);
        return bills;
    }

    @Transactional
    public Bill generateBillForMeter(Meter meter, BillingCycle cycle) {
        int year = cycle.getBillingYear();
        int month = cycle.getBillingMonth();

        // Bills cannot be generated without a valid meter reading for the billing period
        MeterReading reading = meterReadingRepository
            .findByMeterIdAndReadingYearAndReadingMonth(meter.getId(), year, month)
            .orElseThrow(() -> new BusinessException(
                "No meter reading found for meter " + meter.getMeterNumber() +
                " in " + year + "/" + month + ". A reading is required before generating a bill."));

        double consumption = reading.getConsumption().doubleValue();

        Tariff tariff = meter.getTariff();
        if (tariff == null) {
            List<Tariff> matchingTariffs = tariffRepository
                .findByUtilityTypeAndActiveTrueAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                    meter.getMeterType(), LocalDate.now());
            tariff = matchingTariffs.isEmpty() ? null : matchingTariffs.get(0);
        }

        BigDecimal consumptionAmount = calculateConsumptionCharge(tariff, consumption, meter.getMeterType(), LocalDate.now());
        List<ServiceCharge> serviceCharges = serviceChargeRepository.findByUtilityTypeAndActiveTrue(meter.getMeterType());
        List<Tax> taxes = taxRepository.findByUtilityTypeAndActiveTrue(meter.getMeterType());

        BigDecimal serviceChargeTotal = serviceCharges.stream()
            .map(ServiceCharge::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxableAmount = consumptionAmount.add(serviceChargeTotal);
        BigDecimal taxTotal = taxes.stream()
            .map(tax -> taxableAmount.multiply(tax.getRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = consumptionAmount.add(serviceChargeTotal).add(taxTotal);

        String billNumber = generateBillNumber(meter.getMeterType(), year, month);

        Bill bill = Bill.builder()
            .billNumber(billNumber)
            .customer(meter.getCustomer())
            .meter(meter)
            .billingCycle(cycle)
            .meterReading(reading)
            .utilityType(meter.getMeterType())
            .consumptionAmount(consumptionAmount)
            .serviceChargeAmount(serviceChargeTotal)
            .taxAmount(taxTotal)
            .penaltyAmount(BigDecimal.ZERO)
            .totalAmount(totalAmount)
            .paidAmount(BigDecimal.ZERO)
            .balanceAmount(totalAmount)
            .status(BillStatus.PENDING)
            .billDate(LocalDate.now())
            .dueDate(cycle.getDueDate())
            .build();

        bill = billRepository.save(bill);

        List<BillItem> items = buildBillItems(bill, consumption, consumptionAmount, serviceCharges, taxes);
        billItemRepository.saveAll(items);

        log.info("Generated bill {} for meter {} (consumption: {}, total: {})",
            billNumber, meter.getMeterNumber(), consumption, totalAmount);

        return bill;
    }

    public BigDecimal calculateConsumptionCharge(Tariff tariff, double consumption, MeterType type, LocalDate date) {
        if (tariff == null || consumption == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal consumptionBD = BigDecimal.valueOf(consumption);

        if (tariff.getTariffType() == TariffType.FLAT) {
            BigDecimal rate = tariff.getFlatRate() != null ? tariff.getFlatRate() : BigDecimal.ZERO;
            return consumptionBD.multiply(rate).setScale(4, RoundingMode.HALF_UP);
        }

        // Tiered calculation
        List<TariffVersion> tiers = tariffVersionRepository
            .findByTariffIdAndEffectiveDateLessThanEqualOrderByTierOrder(tariff.getId(), date);

        if (tiers.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalCharge = BigDecimal.ZERO;
        BigDecimal remaining = consumptionBD;

        for (TariffVersion tier : tiers) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal tierMin = tier.getMinUnits() != null ? tier.getMinUnits() : BigDecimal.ZERO;
            BigDecimal tierMax = tier.getMaxUnits();
            BigDecimal tierUnits;

            if (tierMax == null) {
                tierUnits = remaining;
            } else {
                BigDecimal tierCapacity = tierMax.subtract(tierMin);
                tierUnits = remaining.min(tierCapacity);
            }

            totalCharge = totalCharge.add(tierUnits.multiply(tier.getRatePerUnit()));
            remaining = remaining.subtract(tierUnits);
        }

        return totalCharge.setScale(4, RoundingMode.HALF_UP);
    }

    @Transactional
    public void applyPenaltiesToOverdueBills() {
        List<Bill> overdueBills = billRepository.findOverdueBills(LocalDate.now());
        log.info("Applying penalties to {} overdue bills", overdueBills.size());

        for (Bill bill : overdueBills) {
            List<Penalty> penalties = penaltyRepository.findByUtilityTypeAndActiveTrue(bill.getUtilityType());
            BigDecimal penaltyTotal = BigDecimal.ZERO;

            for (Penalty penalty : penalties) {
                if (penalty.isPercentage()) {
                    penaltyTotal = penaltyTotal.add(
                        bill.getBalanceAmount().multiply(penalty.getRate())
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    );
                } else if (penalty.getFixedAmount() != null) {
                    penaltyTotal = penaltyTotal.add(penalty.getFixedAmount());
                }
            }

            if (penaltyTotal.compareTo(BigDecimal.ZERO) > 0) {
                bill.setPenaltyAmount(bill.getPenaltyAmount().add(penaltyTotal));
                bill.setTotalAmount(bill.getTotalAmount().add(penaltyTotal));
                bill.setBalanceAmount(bill.getBalanceAmount().add(penaltyTotal));
                bill.setStatus(BillStatus.OVERDUE);
                billRepository.save(bill);
                log.debug("Applied penalty {} to bill {}", penaltyTotal, bill.getBillNumber());
            }
        }
    }

    private List<BillItem> buildBillItems(Bill bill, double consumption, BigDecimal consumptionAmount,
                                           List<ServiceCharge> serviceCharges, List<Tax> taxes) {
        List<BillItem> items = new ArrayList<>();

        if (consumptionAmount.compareTo(BigDecimal.ZERO) > 0) {
            items.add(BillItem.builder()
                .bill(bill)
                .itemType(BillItemType.CONSUMPTION)
                .description("Consumption Charge (" + String.format("%.2f", consumption) + " units)")
                .quantity(BigDecimal.valueOf(consumption))
                .amount(consumptionAmount)
                .build());
        }

        for (ServiceCharge sc : serviceCharges) {
            items.add(BillItem.builder()
                .bill(bill)
                .itemType(BillItemType.SERVICE_CHARGE)
                .description(sc.getName())
                .quantity(BigDecimal.ONE)
                .unitPrice(sc.getAmount())
                .amount(sc.getAmount())
                .build());
        }

        for (Tax tax : taxes) {
            BigDecimal taxAmount = consumptionAmount.add(
                serviceCharges.stream().map(ServiceCharge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
            ).multiply(tax.getRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            items.add(BillItem.builder()
                .bill(bill)
                .itemType(BillItemType.TAX)
                .description(tax.getName() + " (" + tax.getRate() + "%)")
                .quantity(BigDecimal.ONE)
                .unitPrice(taxAmount)
                .amount(taxAmount)
                .build());
        }

        return items;
    }

    private BillingCycle getOrCreateBillingCycle(int year, int month) {
        return billingCycleRepository.findByBillingYearAndBillingMonth(year, month)
            .orElseGet(() -> {
                YearMonth ym = YearMonth.of(year, month);
                LocalDate start = ym.atDay(1);
                LocalDate end = ym.atEndOfMonth();
                LocalDate dueDate = ym.plusMonths(1).atDay(15);

                return billingCycleRepository.save(BillingCycle.builder()
                    .billingYear(year)
                    .billingMonth(month)
                    .startDate(start)
                    .endDate(end)
                    .dueDate(dueDate)
                    .build());
            });
    }

    private String generateBillNumber(MeterType type, int year, int month) {
        String prefix = type == MeterType.WATER ? "WB" : "EB";
        return String.format("%s-%d%02d-%s", prefix, year, month,
            UUID.randomUUID().toString().substring(0, 6).toUpperCase());
    }
}
