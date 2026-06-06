package com.exam.utility.dto.response.notification;

import com.exam.utility.enums.NotificationStatus;
import com.exam.utility.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationStatus status;
    private boolean emailSent;
    private String referenceLink;
    private LocalDateTime createdAt;
}
