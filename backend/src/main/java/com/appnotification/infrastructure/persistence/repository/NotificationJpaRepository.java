package com.appnotification.infrastructure.persistence.repository;

import com.appnotification.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    @Query("SELECT n FROM NotificationJpaEntity n JOIN FETCH n.sender WHERE n.recipient.id = :recipientId ORDER BY n.createdAt DESC")
    Page<NotificationJpaEntity> findByRecipientIdOrderByCreatedAtDesc(
            @Param("recipientId") UUID recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(UUID recipientId);

    long countByRecipientId(UUID recipientId);
}
