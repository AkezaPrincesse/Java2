package com.exam.utility.repository;

import com.exam.utility.entity.AuditLog;
import com.exam.utility.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByPerformedBy(String performedBy, Pageable pageable);
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);
    Page<AuditLog> findByEntityName(String entityName, Pageable pageable);
    Page<AuditLog> findByPerformedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
