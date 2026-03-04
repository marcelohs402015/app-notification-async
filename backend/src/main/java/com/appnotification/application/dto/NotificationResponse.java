package com.appnotification.application.dto;

import com.appnotification.domain.model.Notification;
import com.appnotification.domain.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UserResponse sender,
        String message,
        NotificationType type,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                UserResponse.from(notification.getSender()),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
