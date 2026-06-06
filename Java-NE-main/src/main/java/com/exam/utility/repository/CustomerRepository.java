package com.exam.utility.repository;

import com.exam.utility.entity.Customer;
import com.exam.utility.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByNationalId(String nationalId);
    Optional<Customer> findByEmail(String email);
    boolean existsByNationalId(String nationalId);
    boolean existsByEmail(String email);

    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.fullName) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%',:keyword,'%')) OR " +
           "c.nationalId LIKE CONCAT('%',:keyword,'%') OR " +
           "c.phoneNumber LIKE CONCAT('%',:keyword,'%')")
    Page<Customer> searchCustomers(String keyword, Pageable pageable);

    long countByStatus(CustomerStatus status);
}
