package com.appnotification.presentation.adapter.dto;

import com.appnotification.domain.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        SenderResponse sender,
        String message,
        NotificationType type,
        boolean read,
        Instant createdAt
) {
    public record SenderResponse(UUID id, String username) {}
}
