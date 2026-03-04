package com.appnotification.presentation;

import com.appnotification.application.dto.NotificationResponse;
import com.appnotification.application.dto.PageResponse;
import com.appnotification.application.dto.SendNotificationRequest;
import com.appnotification.application.service.NotificationService;
import com.appnotification.domain.model.User;
import com.appnotification.infrastructure.messaging.NotificationListenerManager;
import com.appnotification.infrastructure.sse.SseEmitterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final NotificationListenerManager listenerManager;

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(notificationService.send(currentUser.getId(), request));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId();
        listenerManager.startListening(userId);
        return sseEmitterRegistry.register(userId);
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.findByRecipient(currentUser.getId(), page, size));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUser.getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(notificationService.countUnread(currentUser.getId()));
    }
}
