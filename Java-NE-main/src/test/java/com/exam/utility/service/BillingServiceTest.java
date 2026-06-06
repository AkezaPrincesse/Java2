package com.exam.utility.service;

import com.exam.utility.billing.BillingEngine;
import com.exam.utility.dto.request.billing.ApproveBillRequest;
import com.exam.utility.dto.response.billing.BillResponse;
import com.exam.utility.entity.*;
import com.exam.utility.enums.BillStatus;
import com.exam.utility.enums.CustomerStatus;
import com.exam.utility.enums.MeterType;
import com.exam.utility.exception.BusinessException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.BillRepository;
import com.exam.utility.repository.CustomerRepository;
import com.exam.utility.repository.UserRepository;
import com.exam.utility.service.impl.BillingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingService Unit Tests")
class BillingServiceTest {

    @Mock BillingEngine billingEngine;
    @Mock BillRepository billRepository;
    @Mock CustomerRepository customerRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock AuditService auditService;

    @InjectMocks BillingServiceImpl billingService;

    private Bill testBill;
    private Customer testCustomer;
    private Meter testMeter;
    private BillingCycle testCycle;

    @BeforeEach
    void setUp() {
        // id is in BaseEntity – set via setter after building
        testCustomer = Customer.builder()
            .fullName("Bob Mugisha")
            .email("bob@example.com")
            .phoneNumber("+250788000001")
            .address("Kigali")
            .status(CustomerStatus.ACTIVE)
            .build();
        testCustomer.setId(1L);

        testMeter = Meter.builder()
            .meterNumber("WM-001")
            .meterType(MeterType.WATER)
            .customer(testCustomer)
            .build();
        testMeter.setId(1L);

        testCycle = BillingCycle.builder()
            .billingYear(2025).billingMonth(12)
            .startDate(LocalDate.of(2025, 12, 1))
            .endDate(LocalDate.of(2025, 12, 31))
            .dueDate(LocalDate.of(2026, 1, 15))
            .build();
        testCycle.setId(1L);

        testBill = Bill.builder()
            .billNumber("WB-202512-AABB12")
            .customer(testCustomer)
            .meter(testMeter)
            .billingCycle(testCycle)
            .utilityType(MeterType.WATER)
            .consumptionAmount(new BigDecimal("5000"))
            .serviceChargeAmount(new BigDecimal("500"))
            .taxAmount(new BigDecimal("990"))
            .penaltyAmount(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("6490"))
            .paidAmount(BigDecimal.ZERO)
            .balanceAmount(new BigDecimal("6490"))
            .status(BillStatus.PENDING)
            .billDate(LocalDate.of(2025, 12, 31))
            .dueDate(LocalDate.of(2026, 1, 15))
            .build();
        testBill.setId(1L);
    }

    @Test
    @DisplayName("Approve bill – should change status to APPROVED when PENDING")
    void approveBill_ShouldApprove_WhenStatusIsPending() {
        ApproveBillRequest req = new ApproveBillRequest();
        req.setBillNumber("WB-202512-AABB12");

        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn("admin@wasac-reg.rw");
        SecurityContextHolder.setContext(ctx);

        when(billRepository.findByBillNumber("WB-202512-AABB12")).thenReturn(Optional.of(testBill));
        when(billRepository.save(any())).thenReturn(testBill);
        doNothing().when(notificationService).notifyBillApproved(any());

        BillResponse response = billingService.approveBill(req);

        assertThat(testBill.getStatus()).isEqualTo(BillStatus.APPROVED);
        assertThat(testBill.getApprovedBy()).isEqualTo("admin@wasac-reg.rw");
        verify(billRepository).save(testBill);
    }

    @Test
    @DisplayName("Approve bill – should throw BusinessException when already APPROVED")
    void approveBill_ShouldThrow_WhenNotPending() {
        testBill.setStatus(BillStatus.APPROVED);

        ApproveBillRequest req = new ApproveBillRequest();
        req.setBillNumber("WB-202512-AABB12");

        when(billRepository.findByBillNumber("WB-202512-AABB12")).thenReturn(Optional.of(testBill));

        assertThatThrownBy(() -> billingService.approveBill(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("GetBillByNumber – should throw when bill not found")
    void getBillByNumber_ShouldThrow_WhenNotFound() {
        when(billRepository.findByBillNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.getBillByNumber("UNKNOWN"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Cancel bill – should throw when bill is PAID")
    void cancelBill_ShouldThrow_WhenPaid() {
        testBill.setStatus(BillStatus.PAID);
        when(billRepository.findById(1L)).thenReturn(Optional.of(testBill));

        assertThatThrownBy(() -> billingService.cancelBill(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("paid");
    }
}
