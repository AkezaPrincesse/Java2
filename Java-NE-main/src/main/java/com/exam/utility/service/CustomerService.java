package com.exam.utility.service;

import com.exam.utility.dto.request.customer.CreateCustomerRequest;
import com.exam.utility.dto.request.customer.UpdateCustomerRequest;
import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.customer.CustomerResponse;
import com.exam.utility.enums.CustomerStatus;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    CustomerResponse create(CreateCustomerRequest request);
    CustomerResponse getById(Long id);
    CustomerResponse getByNationalId(String nationalId);
    PagedResponse<CustomerResponse> getAll(Pageable pageable);
    PagedResponse<CustomerResponse> search(String keyword, Pageable pageable);
    PagedResponse<CustomerResponse> getByStatus(CustomerStatus status, Pageable pageable);
    CustomerResponse update(Long id, UpdateCustomerRequest request);
    void delete(Long id);
    void updateStatus(Long id, CustomerStatus status);
}
