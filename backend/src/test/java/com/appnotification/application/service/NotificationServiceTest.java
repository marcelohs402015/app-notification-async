package com.appnotification.application.service;

import com.appnotification.application.port.input.notification.*;
import com.appnotification.application.port.output.NotificationPublisherPort;
import com.appnotification.application.port.output.NotificationRepositoryPort;
import com.appnotification.application.port.output.UserRepositoryPort;
import com.appnotification.application.usecase.notification.*;
import com.appnotification.domain.entity.Notification;
import com.appnotification.domain.entity.NotificationType;
import com.appnotification.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepositoryPort notificationRepository;
    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private NotificationPublisherPort notificationPublisher;

    private SendNotificationUseCase sendNotificationUseCase;
    private GetNotificationsUseCase getNotificationsUseCase;
    private MarkNotificationAsReadUseCase markAsReadUseCase;
    private CountUnreadNotificationsUseCase countUnreadUseCase;

    private User mockSender;
    private User mockRecipient;
    private Notification mockNotification;

    @BeforeEach
    void setUp() {
        sendNotificationUseCase = new SendNotificationUseCase(notificationRepository, userRepository, notificationPublisher);
        getNotificationsUseCase = new GetNotificationsUseCase(notificationRepository);
        markAsReadUseCase = new MarkNotificationAsReadUseCase(notificationRepository);
        countUnreadUseCase = new CountUnreadNotificationsUseCase(notificationRepository);

        mockSender = new User(UUID.randomUUID(), "maria", "maria@email.com", "hashed", Instant.now());
        mockRecipient = new User(UUID.randomUUID(), "joao", "joao@email.com", "hashed", Instant.now());
        mockNotification = new Notification(UUID.randomUUID(), mockSender, mockRecipient,
                "Olá!", NotificationType.INFO, false, Instant.now());
    }

    @Test
    void send_shouldPersistAndPublish() {
        SendNotificationPort.Command inputCommand = new SendNotificationPort.Command(
                mockSender.getId(), mockRecipient.getId(), "Olá!", NotificationType.INFO);
        when(userRepository.findById(mockSender.getId())).thenReturn(Optional.of(mockSender));
        when(userRepository.findById(mockRecipient.getId())).thenReturn(Optional.of(mockRecipient));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        SendNotificationPort.Result actualResult = sendNotificationUseCase.execute(inputCommand);

        assertThat(actualResult.message()).isEqualTo("Olá!");
        assertThat(actualResult.type()).isEqualTo(NotificationType.INFO);
        assertThat(actualResult.read()).isFalse();
        assertThat(actualResult.senderUsername()).isEqualTo("maria");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getSender()).isEqualTo(mockSender);
        assertThat(captor.getValue().getRecipient()).isEqualTo(mockRecipient);
        verify(notificationPublisher).publish(eq(mockRecipient.getId()), any(Notification.class));
    }

    @Test
    void send_shouldThrow_whenSenderNotFound() {
        UUID unknownSenderId = UUID.randomUUID();
        SendNotificationPort.Command inputCommand = new SendNotificationPort.Command(
                unknownSenderId, mockRecipient.getId(), "Olá!", NotificationType.INFO);
        when(userRepository.findById(unknownSenderId))
                .thenThrow(new IllegalArgumentException("User not found: " + unknownSenderId));

        assertThatThrownBy(() -> sendNotificationUseCase.execute(inputCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(notificationRepository, never()).save(any());
        verify(notificationPublisher, never()).publish(any(), any());
    }

    @Test
    void getNotifications_shouldReturnPagedItems() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                eq(mockRecipient.getId()), eq(0), eq(20)))
                .thenReturn(List.of(mockNotification));
        when(notificationRepository.countTotalByRecipientId(mockRecipient.getId())).thenReturn(1L);

        GetNotificationsPort.Result actualResult = getNotificationsUseCase.execute(
                new GetNotificationsPort.Query(mockRecipient.getId(), 0, 20));

        assertThat(actualResult.content()).hasSize(1);
        assertThat(actualResult.totalElements()).isEqualTo(1);
        assertThat(actualResult.content().get(0).message()).isEqualTo("Olá!");
    }

    @Test
    void getNotifications_shouldReturnEmptyPage_whenNoNotificationsExist() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                eq(mockRecipient.getId()), eq(0), eq(20)))
                .thenReturn(List.of());
        when(notificationRepository.countTotalByRecipientId(mockRecipient.getId())).thenReturn(0L);

        GetNotificationsPort.Result actualResult = getNotificationsUseCase.execute(
                new GetNotificationsPort.Query(mockRecipient.getId(), 0, 20));

        assertThat(actualResult.content()).isEmpty();
        assertThat(actualResult.totalElements()).isZero();
    }

    @Test
    void markAsRead_shouldSetReadTrue_whenRecipientMatches() {
        when(notificationRepository.findById(mockNotification.getId()))
                .thenReturn(Optional.of(mockNotification));
        when(notificationRepository.save(mockNotification)).thenReturn(mockNotification);

        MarkNotificationAsReadPort.Result actualResult = markAsReadUseCase.execute(
                new MarkNotificationAsReadPort.Command(mockNotification.getId(), mockRecipient.getId()));

        assertThat(actualResult.read()).isTrue();
        verify(notificationRepository).save(mockNotification);
    }

    @Test
    void markAsRead_shouldThrow_whenNotificationNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(notificationRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> markAsReadUseCase.execute(
                new MarkNotificationAsReadPort.Command(unknownId, mockRecipient.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Notification not found");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_shouldThrow_whenCallerIsNotRecipient() {
        UUID intruderId = UUID.randomUUID();
        when(notificationRepository.findById(mockNotification.getId()))
                .thenReturn(Optional.of(mockNotification));

        assertThatThrownBy(() -> markAsReadUseCase.execute(
                new MarkNotificationAsReadPort.Command(mockNotification.getId(), intruderId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Access denied");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void countUnread_shouldReturnCorrectCount() {
        when(notificationRepository.countByRecipientIdUnread(mockRecipient.getId())).thenReturn(5L);

        long actualCount = countUnreadUseCase.execute(mockRecipient.getId());

        assertThat(actualCount).isEqualTo(5L);
    }
}
