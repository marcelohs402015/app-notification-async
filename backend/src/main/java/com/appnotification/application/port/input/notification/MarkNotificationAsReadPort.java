package com.appnotification.application.port.input.notification;

import com.appnotification.domain.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public interface MarkNotificationAsReadPort {
    record Command(UUID notificationId, UUID recipientId) {}
    record Result(UUID id, UUID senderId, String senderUsername, String message,
                  NotificationType type, boolean read, Instant createdAt) {}

    Result execute(Command command);
}
