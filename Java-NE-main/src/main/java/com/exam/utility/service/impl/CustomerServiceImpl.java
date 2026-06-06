package com.exam.utility.service.impl;

import com.exam.utility.dto.request.customer.CreateCustomerRequest;
import com.exam.utility.dto.request.customer.UpdateCustomerRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.customer.CustomerResponse;
import com.exam.utility.entity.Customer;
import com.exam.utility.enums.AuditAction;
import com.exam.utility.enums.CustomerStatus;
import com.exam.utility.exception.BusinessException;
import com.exam.utility.exception.DuplicateResourceException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.CustomerRepository;
import com.exam.utility.service.AuditService;
import com.exam.utility.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages customer account creation and lifecycle for the utility billing system.
 *
 * Business rules enforced:
 * - National ID must be exactly 16 digits and unique system-wide.
 * - Email (when provided) must be unique.
 * - Phone number must follow the Rwandan format: 078/079/072/073 + 7 digits.
 * - Duplicate registration is detected via National ID, email, and phone number.
 * - New customers default to ACTIVE status.
 * - INACTIVE customers cannot receive new bills or meter readings.
 * - Customer records are never physically deleted (soft status change only).
 * - All creation and update events are audit-logged with the acting administrator.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (customerRepository.existsByNationalId(request.getNationalId())) {
            throw new DuplicateResourceException("Customer", "nationalId", request.getNationalId());
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer", "email", request.getEmail());
        }

        Customer customer = Customer.builder()
            .fullName(request.getFullName())
            .nationalId(request.getNationalId())
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .address(request.getAddress())
            .district(request.getDistrict())
            .sector(request.getSector())
            .status(CustomerStatus.ACTIVE)
            .build();

        customer = customerRepository.save(customer);
        auditService.log(AuditAction.CREATE, "Customer", customer.getId().toString(),
            "Customer created: " + customer.getFullName());
        log.info("Customer created: {} (ID: {})", customer.getFullName(), customer.getId());
        return toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getByNationalId(String nationalId) {
        Customer c = customerRepository.findByNationalId(nationalId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", "nationalId", nationalId));
        return toResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CustomerResponse> getAll(Pageable pageable) {
        return PagedResponse.of(customerRepository.findAll(pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CustomerResponse> search(String keyword, Pageable pageable) {
        return PagedResponse.of(customerRepository.searchCustomers(keyword, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CustomerResponse> getByStatus(CustomerStatus status, Pageable pageable) {
        return PagedResponse.of(customerRepository.findByStatus(status, pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public CustomerResponse update(Long id, UpdateCustomerRequest request) {
        Customer customer = findById(id);
        String oldValues = customer.toString();

        if (request.getFullName() != null) customer.setFullName(request.getFullName());
        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail())) {
            if (customerRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Customer", "email", request.getEmail());
            }
            customer.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) customer.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getDistrict() != null) customer.setDistrict(request.getDistrict());
        if (request.getSector() != null) customer.setSector(request.getSector());
        if (request.getStatus() != null) customer.setStatus(request.getStatus());

        customer = customerRepository.save(customer);
        auditService.log(AuditAction.UPDATE, "Customer", customer.getId().toString(),
            oldValues, customer.toString(), "Customer updated");
        return toResponse(customer);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Customer customer = findById(id);
        if (!customer.getBills().isEmpty()) {
            throw new BusinessException("Cannot delete customer with existing bills");
        }
        customerRepository.delete(customer);
        auditService.log(AuditAction.DELETE, "Customer", id.toString(), "Customer deleted");
        log.info("Customer deleted: {}", id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, CustomerStatus status) {
        Customer customer = findById(id);
        customer.setStatus(status);
        customerRepository.save(customer);
        auditService.log(AuditAction.UPDATE, "Customer", id.toString(),
            "Customer status changed to: " + status);
    }

    private Customer findById(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
            .id(c.getId())
            .fullName(c.getFullName())
            .nationalId(c.getNationalId())
            .email(c.getEmail())
            .phoneNumber(c.getPhoneNumber())
            .address(c.getAddress())
            .district(c.getDistrict())
            .sector(c.getSector())
            .status(c.getStatus())
            .totalMeters(c.getMeters().size())
            .createdAt(c.getCreatedAt())
            .updatedAt(c.getUpdatedAt())
            .createdBy(c.getCreatedBy())
            .build();
    }
}
