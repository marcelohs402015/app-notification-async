package com.appnotification.application.usecase.notification;

import com.appnotification.application.port.input.notification.GetNotificationsPort;
import com.appnotification.application.port.output.NotificationRepositoryPort;
import com.appnotification.domain.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetNotificationsUseCase implements GetNotificationsPort {

    private final NotificationRepositoryPort notificationRepository;

    @Transactional(readOnly = true)
    @Override
    public Result execute(Query query) {
        List<Notification> notifications = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(query.recipientId(), query.page(), query.size());

        long total = notificationRepository.countTotalByRecipientId(query.recipientId());
        int totalPages = (int) Math.ceil((double) total / query.size());
        boolean isLast = (query.page() + 1) >= totalPages;

        List<NotificationItem> items = notifications.stream().map(this::toItem).toList();

        return new Result(items, query.page(), query.size(), total, totalPages, isLast);
    }

    private NotificationItem toItem(Notification n) {
        return new NotificationItem(n.getId(), n.getSender().getId(), n.getSender().getUsername(),
                n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt());
    }
}
