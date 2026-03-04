package com.appnotification.presentation.adapter.dto;

import com.appnotification.domain.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendNotificationRequest(
        @NotNull UUID recipientId,
        @NotBlank @Size(max = 500) String message,
        @NotNull NotificationType type
) {}
