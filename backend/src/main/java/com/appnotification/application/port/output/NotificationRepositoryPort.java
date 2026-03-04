package com.appnotification.application.port.output;

import com.appnotification.domain.entity.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    Optional<Notification> findById(UUID id);
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, int page, int size);
    long countByRecipientIdUnread(UUID recipientId);
    long countTotalByRecipientId(UUID recipientId);
}
