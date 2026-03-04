package com.appnotification.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final Instant createdAt;

    public User(UUID id, String username, String email, String passwordHash, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
