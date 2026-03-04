package com.appnotification.application.service;

import com.appnotification.application.dto.NotificationResponse;
import com.appnotification.application.dto.PageResponse;
import com.appnotification.application.dto.SendNotificationRequest;
import com.appnotification.domain.model.Notification;
import com.appnotification.domain.model.User;
import com.appnotification.domain.repository.NotificationRepository;
import com.appnotification.infrastructure.messaging.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public NotificationResponse send(UUID senderId, SendNotificationRequest request) {
        User sender = userService.findById(senderId);
        User recipient = userService.findById(request.recipientId());

        Notification notification = new Notification();
        notification.setSender(sender);
        notification.setRecipient(recipient);
        notification.setMessage(request.message());
        notification.setType(request.type());

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = NotificationResponse.from(saved);

        notificationPublisher.publish(recipient.getId(), response);

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> findByRecipient(UUID recipientId, int page, int size) {
        Page<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                recipientId, PageRequest.of(page, size)
        );
        return PageResponse.from(notifications, NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID recipientId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getRecipient().getId().equals(recipientId)) {
            throw new IllegalArgumentException("Access denied");
        }

        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }
}
