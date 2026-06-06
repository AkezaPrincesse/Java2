package com.exam.utility.service;

import com.exam.utility.dto.request.payment.CreatePaymentRequest;
import com.exam.utility.dto.response.payment.PaymentResponse;
import com.exam.utility.entity.*;
import com.exam.utility.enums.*;
import com.exam.utility.exception.BusinessException;
import com.exam.utility.repository.BillRepository;
import com.exam.utility.repository.PaymentRepository;
import com.exam.utility.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock BillRepository billRepository;
    @Mock NotificationService notificationService;
    @Mock AuditService auditService;

    @InjectMocks PaymentServiceImpl paymentService;

    private Bill approvedBill;
    private Customer testCustomer;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        // id is in BaseEntity – set it via setter after building
        testCustomer = Customer.builder()
            .fullName("Eve Munezero")
            .email("eve@example.com")
            .phoneNumber("+250788000002")
            .address("Kigali")
            .status(CustomerStatus.ACTIVE)
            .build();
        testCustomer.setId(1L);

        Meter meter = Meter.builder()
            .meterNumber("EM-001")
            .meterType(MeterType.ELECTRICITY)
            .customer(testCustomer)
            .build();
        meter.setId(1L);

        BillingCycle cycle = BillingCycle.builder()
            .billingYear(2025).billingMonth(12)
            .startDate(LocalDate.of(2025, 12, 1))
            .endDate(LocalDate.of(2025, 12, 31))
            .dueDate(LocalDate.of(2026, 1, 15))
            .build();
        cycle.setId(1L);

        approvedBill = Bill.builder()
            .billNumber("EB-202512-TEST01")
            .customer(testCustomer)
            .meter(meter)
            .billingCycle(cycle)
            .utilityType(MeterType.ELECTRICITY)
            .consumptionAmount(new BigDecimal("10000"))
            .serviceChargeAmount(new BigDecimal("750"))
            .taxAmount(new BigDecimal("1935"))
            .penaltyAmount(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("12685"))
            .paidAmount(BigDecimal.ZERO)
            .balanceAmount(new BigDecimal("12685"))
            .status(BillStatus.APPROVED)
            .billDate(LocalDate.of(2025, 12, 31))
            .dueDate(LocalDate.of(2026, 1, 15))
            .build();
        approvedBill.setId(1L);

        testPayment = Payment.builder()
            .receiptNumber("RCP-TESTRCPT")
            .bill(approvedBill)
            .customer(testCustomer)
            .amount(new BigDecimal("12685"))
            .paymentMethod(PaymentMethod.MOMO)
            .paymentStatus(PaymentStatus.COMPLETED)
            .paymentDate(LocalDateTime.now())
            .build();
        testPayment.setId(1L);
    }

    @Test
    @DisplayName("ProcessPayment – should process and mark bill as PAID when full amount")
    void processPayment_ShouldMarkBillPaid_WhenFullAmount() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setBillNumber("EB-202512-TEST01");
        req.setAmount(new BigDecimal("12685"));
        req.setPaymentMethod(PaymentMethod.MOMO);

        when(billRepository.findByBillNumber("EB-202512-TEST01")).thenReturn(Optional.of(approvedBill));
        when(paymentRepository.save(any())).thenReturn(testPayment);
        when(billRepository.save(any())).thenReturn(approvedBill);
        doNothing().when(notificationService).notifyPaymentReceived(any());

        PaymentResponse response = paymentService.processPayment(req);

        assertThat(response).isNotNull();
        assertThat(approvedBill.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(approvedBill.getBalanceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("ProcessPayment – should mark bill as PARTIALLY_PAID for partial amount")
    void processPayment_ShouldMarkPartiallyPaid_WhenPartialAmount() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setBillNumber("EB-202512-TEST01");
        req.setAmount(new BigDecimal("6000"));
        req.setPaymentMethod(PaymentMethod.CASH);

        when(billRepository.findByBillNumber("EB-202512-TEST01")).thenReturn(Optional.of(approvedBill));
        when(paymentRepository.save(any())).thenReturn(testPayment);
        when(billRepository.save(any())).thenReturn(approvedBill);
        doNothing().when(notificationService).notifyPaymentReceived(any());

        paymentService.processPayment(req);

        assertThat(approvedBill.getStatus()).isEqualTo(BillStatus.PARTIALLY_PAID);
        assertThat(approvedBill.getBalanceAmount()).isEqualByComparingTo(new BigDecimal("6685"));
    }

    @Test
    @DisplayName("ProcessPayment – should throw when amount exceeds balance")
    void processPayment_ShouldThrow_WhenAmountExceedsBalance() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setBillNumber("EB-202512-TEST01");
        req.setAmount(new BigDecimal("99999"));
        req.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        when(billRepository.findByBillNumber("EB-202512-TEST01")).thenReturn(Optional.of(approvedBill));

        assertThatThrownBy(() -> paymentService.processPayment(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("exceeds balance");
    }

    @Test
    @DisplayName("ProcessPayment – should throw when bill is CANCELLED")
    void processPayment_ShouldThrow_WhenBillCancelled() {
        approvedBill.setStatus(BillStatus.CANCELLED);

        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setBillNumber("EB-202512-TEST01");
        req.setAmount(BigDecimal.ONE);
        req.setPaymentMethod(PaymentMethod.CASH);

        when(billRepository.findByBillNumber("EB-202512-TEST01")).thenReturn(Optional.of(approvedBill));

        assertThatThrownBy(() -> paymentService.processPayment(req))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("cancelled");
    }
}
