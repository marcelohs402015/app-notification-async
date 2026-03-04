package com.appnotification.infrastructure.messaging;

import com.appnotification.application.dto.NotificationResponse;
import com.appnotification.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(UUID recipientId, NotificationResponse notification) {
        String routingKey = RabbitMQConfig.ROUTING_KEY_PREFIX + recipientId;
        rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE, routingKey, notification);
    }
}
