package com.appnotification.application.port.input.notification;

import java.util.UUID;

public interface CountUnreadNotificationsPort {
    long execute(UUID recipientId);
}
