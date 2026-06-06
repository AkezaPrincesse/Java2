package com.exam.utility.service;

import com.exam.utility.dto.request.customer.CreateCustomerRequest;
import com.exam.utility.dto.request.customer.UpdateCustomerRequest;
import com.exam.utility.dto.response.customer.CustomerResponse;
import com.exam.utility.entity.Customer;
import com.exam.utility.enums.CustomerStatus;
import com.exam.utility.exception.DuplicateResourceException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.CustomerRepository;
import com.exam.utility.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock AuditService auditService;

    @InjectMocks CustomerServiceImpl customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        // id is in BaseEntity – set via setter after building
        customer = Customer.builder()
            .fullName("Alice Uwimana")
            .nationalId("1199880012345678")
            .email("alice@example.com")
            .phoneNumber("+250788111222")
            .address("KN 5 Ave, Kigali")
            .status(CustomerStatus.ACTIVE)
            .build();
        customer.setId(1L);
    }

    @Test
    @DisplayName("Create – should return CustomerResponse when data is valid")
    void create_ShouldReturnCustomerResponse_WhenValid() {
        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setFullName("Alice Uwimana");
        req.setNationalId("1199880012345678");
        req.setEmail("alice@example.com");
        req.setPhoneNumber("+250788111222");
        req.setAddress("KN 5 Ave, Kigali");

        when(customerRepository.existsByNationalId(anyString())).thenReturn(false);
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(customer);

        CustomerResponse response = customerService.create(req);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        verify(auditService).log(any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Create – should throw DuplicateResourceException when nationalId exists")
    void create_ShouldThrow_WhenNationalIdExists() {
        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setNationalId("1199880012345678");
        req.setEmail("other@email.com");
        req.setFullName("Test");
        req.setPhoneNumber("+250788111222");
        req.setAddress("Address");

        when(customerRepository.existsByNationalId("1199880012345678")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(req))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("GetById – should throw ResourceNotFoundException when customer not found")
    void getById_ShouldThrow_WhenNotFound() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Customer");
    }

    @Test
    @DisplayName("GetAll – should return paginated customers")
    void getAll_ShouldReturnPage() {
        Page<Customer> page = new PageImpl<>(Collections.singletonList(customer));
        when(customerRepository.findAll(any(PageRequest.class))).thenReturn(page);

        var result = customerService.getAll(PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Update – should update customer fields")
    void update_ShouldUpdateFields_WhenValid() {
        UpdateCustomerRequest req = new UpdateCustomerRequest();
        req.setFullName("Alice Updated");
        req.setPhoneNumber("+250788999888");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);

        CustomerResponse response = customerService.update(1L, req);

        assertThat(response).isNotNull();
        verify(customerRepository).save(any());
    }

    @Test
    @DisplayName("UpdateStatus – should change customer status")
    void updateStatus_ShouldChangeStatus() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);

        customerService.updateStatus(1L, CustomerStatus.SUSPENDED);

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
    }
}
