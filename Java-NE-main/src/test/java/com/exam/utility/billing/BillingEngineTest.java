package com.exam.utility.billing;

import com.exam.utility.entity.*;
import com.exam.utility.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exam.utility.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingEngine Unit Tests")
class BillingEngineTest {

    @Mock BillRepository billRepository;
    @Mock BillItemRepository billItemRepository;
    @Mock BillingCycleRepository billingCycleRepository;
    @Mock MeterRepository meterRepository;
    @Mock MeterReadingRepository meterReadingRepository;
    @Mock TariffRepository tariffRepository;
    @Mock TariffVersionRepository tariffVersionRepository;
    @Mock ServiceChargeRepository serviceChargeRepository;
    @Mock TaxRepository taxRepository;
    @Mock PenaltyRepository penaltyRepository;

    @InjectMocks BillingEngine billingEngine;

    private Tariff flatTariff;
    private Tariff tieredTariff;

    @BeforeEach
    void setUp() {
        // id is in BaseEntity – set via setter after building
        flatTariff = Tariff.builder()
            .name("Water Flat")
            .utilityType(MeterType.WATER)
            .tariffType(TariffType.FLAT)
            .flatRate(new BigDecimal("250"))
            .effectiveDate(LocalDate.of(2024, 1, 1))
            .active(true)
            .build();
        flatTariff.setId(1L);

        tieredTariff = Tariff.builder()
            .name("Electricity Tiered")
            .utilityType(MeterType.ELECTRICITY)
            .tariffType(TariffType.TIERED)
            .effectiveDate(LocalDate.of(2024, 1, 1))
            .active(true)
            .build();
        tieredTariff.setId(2L);
    }

    @Test
    @DisplayName("calculateConsumptionCharge – FLAT tariff: 20 units × 250 = 5000")
    void calculateFlat_ShouldReturn5000_For20Units() {
        BigDecimal charge = billingEngine.calculateConsumptionCharge(
            flatTariff, 20.0, MeterType.WATER, LocalDate.now());

        assertThat(charge).isEqualByComparingTo(new BigDecimal("5000.0000"));
    }

    @Test
    @DisplayName("calculateConsumptionCharge – zero consumption returns zero")
    void calculateFlat_ShouldReturnZero_ForZeroConsumption() {
        BigDecimal charge = billingEngine.calculateConsumptionCharge(
            flatTariff, 0.0, MeterType.WATER, LocalDate.now());

        assertThat(charge).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateConsumptionCharge – null tariff returns zero")
    void calculate_ShouldReturnZero_ForNullTariff() {
        BigDecimal charge = billingEngine.calculateConsumptionCharge(
            null, 50.0, MeterType.WATER, LocalDate.now());

        assertThat(charge).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateConsumptionCharge – TIERED: 20×100 + 10×150 = 3500")
    void calculateTiered_ShouldApplyTiers_Correctly() {
        // Tier 1: 0–20 units @ 100 RWF
        TariffVersion tier1 = TariffVersion.builder()
            .tariff(tieredTariff).tierOrder(1)
            .minUnits(BigDecimal.ZERO).maxUnits(new BigDecimal("20"))
            .ratePerUnit(new BigDecimal("100"))
            .effectiveDate(LocalDate.of(2024, 1, 1))
            .build();
        tier1.setId(1L);

        // Tier 2: 20+ units @ 150 RWF (no max = unlimited)
        TariffVersion tier2 = TariffVersion.builder()
            .tariff(tieredTariff).tierOrder(2)
            .minUnits(new BigDecimal("20")).maxUnits(null)
            .ratePerUnit(new BigDecimal("150"))
            .effectiveDate(LocalDate.of(2024, 1, 1))
            .build();
        tier2.setId(2L);

        when(tariffVersionRepository
            .findByTariffIdAndEffectiveDateLessThanEqualOrderByTierOrder(anyLong(), any()))
            .thenReturn(List.of(tier1, tier2));

        // 30 units: 20×100 + 10×150 = 2000 + 1500 = 3500
        BigDecimal charge = billingEngine.calculateConsumptionCharge(
            tieredTariff, 30.0, MeterType.ELECTRICITY, LocalDate.now());

        assertThat(charge).isEqualByComparingTo(new BigDecimal("3500.0000"));
    }
}
