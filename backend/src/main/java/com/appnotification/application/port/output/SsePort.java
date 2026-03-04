package com.appnotification.application.port.output;

import com.appnotification.domain.entity.Notification;

import java.util.UUID;

public interface SsePort {
    void sendToUser(UUID userId, Notification notification);
    boolean isConnected(UUID userId);
}
