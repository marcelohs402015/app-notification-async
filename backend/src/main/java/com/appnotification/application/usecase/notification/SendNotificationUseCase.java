package com.appnotification.application.usecase.notification;

import com.appnotification.application.port.input.notification.SendNotificationPort;
import com.appnotification.application.port.output.NotificationPublisherPort;
import com.appnotification.application.port.output.NotificationRepositoryPort;
import com.appnotification.application.port.output.UserRepositoryPort;
import com.appnotification.domain.entity.Notification;
import com.appnotification.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SendNotificationUseCase implements SendNotificationPort {

    private final NotificationRepositoryPort notificationRepository;
    private final UserRepositoryPort userRepository;
    private final NotificationPublisherPort notificationPublisher;

    @Transactional
    @Override
    public Result execute(Command command) {
        User sender = findUserOrThrow(command.senderId());
        User recipient = findUserOrThrow(command.recipientId());

        Notification notification = new Notification(null, sender, recipient,
                command.message(), command.type(), false, Instant.now());

        Notification saved = notificationRepository.save(notification);
        notificationPublisher.publish(recipient.getId(), saved);

        return toResult(saved);
    }

    private User findUserOrThrow(java.util.UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private Result toResult(Notification n) {
        return new Result(n.getId(), n.getSender().getId(), n.getSender().getUsername(),
                n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt());
    }
}
