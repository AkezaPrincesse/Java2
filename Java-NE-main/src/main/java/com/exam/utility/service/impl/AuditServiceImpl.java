package com.exam.utility.service.impl;

import com.exam.utility.entity.AuditLog;
import com.exam.utility.enums.AuditAction;
import com.exam.utility.repository.AuditLogRepository;
import com.exam.utility.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    public void log(AuditAction action, String entityName, String entityId,
                    String oldValues, String newValues, String description) {
        String performedBy = getCurrentUser();
        auditLogRepository.save(AuditLog.builder()
            .performedBy(performedBy)
            .performedAt(LocalDateTime.now())
            .action(action)
            .entityName(entityName)
            .entityId(entityId)
            .oldValues(oldValues)
            .newValues(newValues)
            .description(description)
            .build());
    }

    @Override
    @Async
    public void log(AuditAction action, String entityName, String entityId, String description) {
        log(action, entityName, entityId, null, null, description);
    }

    @Override
    @Async
    public void logLogin(String email, boolean success, HttpServletRequest request) {
        String ip = getClientIp(request);
        String ua = request.getHeader("User-Agent");
        auditLogRepository.save(AuditLog.builder()
            .performedBy(email)
            .performedAt(LocalDateTime.now())
            .action(success ? AuditAction.LOGIN : AuditAction.LOGIN_FAILED)
            .entityName("User")
            .entityId(email)
            .ipAddress(ip)
            .userAgent(ua)
            .description(success ? "Successful login" : "Failed login attempt")
            .build());
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
