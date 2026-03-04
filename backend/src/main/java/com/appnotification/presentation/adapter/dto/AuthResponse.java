package com.appnotification.presentation.adapter.dto;

import java.time.Instant;

public record AuthResponse(String token, UserResponse user) {

    public record UserResponse(String id, String username, String email, Instant createdAt) {}
}
