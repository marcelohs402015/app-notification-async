package com.appnotification.presentation.adapter.controller;

import com.appnotification.application.port.input.notification.CountUnreadNotificationsPort;
import com.appnotification.application.port.input.notification.GetNotificationsPort;
import com.appnotification.application.port.input.notification.MarkNotificationAsReadPort;
import com.appnotification.application.port.input.notification.SendNotificationPort;
import com.appnotification.domain.entity.User;
import com.appnotification.infrastructure.messaging.NotificationListenerManager;
import com.appnotification.infrastructure.sse.SseEmitterRegistry;
import com.appnotification.presentation.adapter.dto.NotificationResponse;
import com.appnotification.presentation.adapter.dto.PageResponse;
import com.appnotification.presentation.adapter.dto.SendNotificationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SendNotificationPort sendNotification;
    private final GetNotificationsPort getNotifications;
    private final MarkNotificationAsReadPort markAsRead;
    private final CountUnreadNotificationsPort countUnread;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final NotificationListenerManager listenerManager;

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> send(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SendNotificationRequest request) {
        SendNotificationPort.Result result = sendNotification.execute(
                new SendNotificationPort.Command(currentUser.getId(), request.recipientId(),
                        request.message(), request.type()));
        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId();
        listenerManager.startListening(userId);
        return sseEmitterRegistry.register(userId);
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        GetNotificationsPort.Result result = getNotifications.execute(
                new GetNotificationsPort.Query(currentUser.getId(), page, size));

        List<NotificationResponse> content = result.content().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(new PageResponse<>(content, result.page(), result.size(),
                result.totalElements(), result.totalPages(), result.last()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markNotificationAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        MarkNotificationAsReadPort.Result result = markAsRead.execute(
                new MarkNotificationAsReadPort.Command(id, currentUser.getId()));
        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(countUnread.execute(currentUser.getId()));
    }

    private NotificationResponse toResponse(SendNotificationPort.Result r) {
        return new NotificationResponse(r.id(),
                new NotificationResponse.SenderResponse(r.senderId(), r.senderUsername()),
                r.message(), r.type(), r.read(), r.createdAt());
    }

    private NotificationResponse toResponse(GetNotificationsPort.NotificationItem r) {
        return new NotificationResponse(r.id(),
                new NotificationResponse.SenderResponse(r.senderId(), r.senderUsername()),
                r.message(), r.type(), r.read(), r.createdAt());
    }

    private NotificationResponse toResponse(MarkNotificationAsReadPort.Result r) {
        return new NotificationResponse(r.id(),
                new NotificationResponse.SenderResponse(r.senderId(), r.senderUsername()),
                r.message(), r.type(), r.read(), r.createdAt());
    }
}
