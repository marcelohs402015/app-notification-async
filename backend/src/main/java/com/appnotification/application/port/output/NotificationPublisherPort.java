package com.appnotification.application.port.output;

import com.appnotification.domain.entity.Notification;

import java.util.UUID;

public interface NotificationPublisherPort {
    void publish(UUID recipientId, Notification notification);
}
