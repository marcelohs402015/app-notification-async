package com.appnotification.infrastructure.messaging;

import com.appnotification.domain.entity.Notification;
import com.appnotification.domain.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationMessage(
        UUID id,
        UUID senderId,
        String senderUsername,
        UUID recipientId,
        String message,
        NotificationType type,
        boolean read,
        Instant createdAt
) {
    public static NotificationMessage from(Notification n) {
        return new NotificationMessage(n.getId(), n.getSender().getId(), n.getSender().getUsername(),
                n.getRecipient().getId(), n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt());
    }
}
