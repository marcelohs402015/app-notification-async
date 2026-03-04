package com.appnotification.infrastructure.messaging;

import com.appnotification.application.dto.NotificationResponse;
import com.appnotification.infrastructure.config.RabbitMQConfig;
import com.appnotification.infrastructure.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListenerManager {

    private final ConnectionFactory connectionFactory;
    private final RabbitAdmin rabbitAdmin;
    private final TopicExchange notificationExchange;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final MessageConverter messageConverter;

    private final Map<UUID, SimpleMessageListenerContainer> activeListeners = new ConcurrentHashMap<>();

    public void startListening(UUID userId) {
        if (activeListeners.containsKey(userId)) {
            return;
        }

        String queueName = RabbitMQConfig.QUEUE_PREFIX + userId;
        String routingKey = RabbitMQConfig.ROUTING_KEY_PREFIX + userId;

        Queue queue = new Queue(queueName, true, false, false);
        rabbitAdmin.declareQueue(queue);

        Binding binding = BindingBuilder.bind(queue).to(notificationExchange).with(routingKey);
        rabbitAdmin.declareBinding(binding);

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(new MessageListenerAdapter(new Object() {
            @SuppressWarnings("unused")
            public void handleMessage(NotificationResponse notification) {
                sseEmitterRegistry.sendToUser(userId, notification);
            }
        }, messageConverter));
        container.start();

        activeListeners.put(userId, container);
        log.info("Started RabbitMQ listener for user {}", userId);
    }

    public void stopListening(UUID userId) {
        SimpleMessageListenerContainer container = activeListeners.remove(userId);
        if (container != null) {
            container.stop();
            log.info("Stopped RabbitMQ listener for user {}", userId);
        }
    }
}
