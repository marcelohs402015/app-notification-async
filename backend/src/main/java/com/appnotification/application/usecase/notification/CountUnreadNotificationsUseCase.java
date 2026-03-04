package com.appnotification.application.usecase.notification;

import com.appnotification.application.port.input.notification.CountUnreadNotificationsPort;
import com.appnotification.application.port.output.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CountUnreadNotificationsUseCase implements CountUnreadNotificationsPort {

    private final NotificationRepositoryPort notificationRepository;

    @Transactional(readOnly = true)
    @Override
    public long execute(UUID recipientId) {
        return notificationRepository.countByRecipientIdUnread(recipientId);
    }
}
