package com.exam.utility.repository;

import com.exam.utility.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    Page<EmailLog> findByToEmail(String toEmail, Pageable pageable);
    Page<EmailLog> findBySent(boolean sent, Pageable pageable);
}
