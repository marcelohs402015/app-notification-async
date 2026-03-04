package com.appnotification.application.usecase.notification;

import com.appnotification.application.port.input.notification.MarkNotificationAsReadPort;
import com.appnotification.application.port.output.NotificationRepositoryPort;
import com.appnotification.domain.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkNotificationAsReadUseCase implements MarkNotificationAsReadPort {

    private final NotificationRepositoryPort notificationRepository;

    @Transactional
    @Override
    public Result execute(Command command) {
        Notification notification = notificationRepository.findById(command.notificationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found: " + command.notificationId()));

        if (!notification.belongsTo(command.recipientId())) {
            throw new IllegalArgumentException("Access denied");
        }

        notification.markAsRead();
        Notification saved = notificationRepository.save(notification);

        return toResult(saved);
    }

    private Result toResult(Notification n) {
        return new Result(n.getId(), n.getSender().getId(), n.getSender().getUsername(),
                n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt());
    }
}
