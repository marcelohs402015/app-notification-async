package com.appnotification.application.port.input.notification;

import com.appnotification.domain.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public interface SendNotificationPort {
    record Command(UUID senderId, UUID recipientId, String message, NotificationType type) {}
    record Result(UUID id, UUID senderId, String senderUsername, String message,
                  NotificationType type, boolean read, Instant createdAt) {}

    Result execute(Command command);
}
