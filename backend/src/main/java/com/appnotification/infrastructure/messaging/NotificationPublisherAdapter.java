package com.appnotification.infrastructure.messaging;

import com.appnotification.application.port.output.NotificationPublisherPort;
import com.appnotification.domain.entity.Notification;
import com.appnotification.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationPublisherAdapter implements NotificationPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(UUID recipientId, Notification notification) {
        String routingKey = RabbitMQConfig.ROUTING_KEY_PREFIX + recipientId;
        NotificationMessage message = NotificationMessage.from(notification);
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE, routingKey, message);
    }
}
