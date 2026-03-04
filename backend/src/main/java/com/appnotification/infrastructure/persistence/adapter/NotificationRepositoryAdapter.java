package com.appnotification.infrastructure.persistence.adapter;

import com.appnotification.application.port.output.NotificationRepositoryPort;
import com.appnotification.domain.entity.Notification;
import com.appnotification.domain.entity.User;
import com.appnotification.infrastructure.persistence.entity.NotificationJpaEntity;
import com.appnotification.infrastructure.persistence.entity.UserJpaEntity;
import com.appnotification.infrastructure.persistence.repository.NotificationJpaRepository;
import com.appnotification.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository notificationJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = toEntity(notification);
        return toDomain(notificationJpaRepository.save(entity));
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return notificationJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, int page, int size) {
        return notificationJpaRepository
                .findByRecipientIdOrderByCreatedAtDesc(recipientId, PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByRecipientIdUnread(UUID recipientId) {
        return notificationJpaRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Override
    public long countTotalByRecipientId(UUID recipientId) {
        return notificationJpaRepository.countByRecipientId(recipientId);
    }

    private Notification toDomain(NotificationJpaEntity e) {
        User sender = new User(e.getSender().getId(), e.getSender().getUsername(),
                e.getSender().getEmail(), e.getSender().getPassword(), e.getSender().getCreatedAt());
        User recipient = new User(e.getRecipient().getId(), e.getRecipient().getUsername(),
                e.getRecipient().getEmail(), e.getRecipient().getPassword(), e.getRecipient().getCreatedAt());
        return new Notification(e.getId(), sender, recipient, e.getMessage(),
                e.getType(), e.isRead(), e.getCreatedAt());
    }

    private NotificationJpaEntity toEntity(Notification n) {
        NotificationJpaEntity e = new NotificationJpaEntity();
        e.setId(n.getId());
        e.setMessage(n.getMessage());
        e.setType(n.getType());
        e.setRead(n.isRead());

        UserJpaEntity sender = userJpaRepository.getReferenceById(n.getSender().getId());
        UserJpaEntity recipient = userJpaRepository.getReferenceById(n.getRecipient().getId());
        e.setSender(sender);
        e.setRecipient(recipient);
        return e;
    }
}
