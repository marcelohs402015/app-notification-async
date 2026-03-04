package com.appnotification.application.port.input.notification;

import com.appnotification.domain.entity.NotificationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GetNotificationsPort {
    record Query(UUID recipientId, int page, int size) {}
    record NotificationItem(UUID id, UUID senderId, String senderUsername, String message,
                            NotificationType type, boolean read, Instant createdAt) {}
    record Result(List<NotificationItem> content, int page, int size,
                  long totalElements, int totalPages, boolean last) {}

    Result execute(Query query);
}
