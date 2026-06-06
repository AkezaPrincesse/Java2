package com.exam.utility.service;

import com.exam.utility.enums.AuditAction;
import jakarta.servlet.http.HttpServletRequest;

public interface AuditService {
    void log(AuditAction action, String entityName, String entityId,
             String oldValues, String newValues, String description);
    void log(AuditAction action, String entityName, String entityId, String description);
    void logLogin(String email, boolean success, HttpServletRequest request);
}
