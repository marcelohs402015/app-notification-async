package com.appnotification.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class Notification {

    private final UUID id;
    private final User sender;
    private final User recipient;
    private final String message;
    private final NotificationType type;
    private boolean read;
    private final Instant createdAt;

    public Notification(UUID id, User sender, User recipient, String message,
                        NotificationType type, boolean read, Instant createdAt) {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.type = type;
        this.read = read;
        this.createdAt = createdAt;
    }

    public void markAsRead() {
        this.read = true;
    }

    public boolean belongsTo(UUID userId) {
        return this.recipient.getId().equals(userId);
    }

    public UUID getId() { return id; }
    public User getSender() { return sender; }
    public User getRecipient() { return recipient; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
}
