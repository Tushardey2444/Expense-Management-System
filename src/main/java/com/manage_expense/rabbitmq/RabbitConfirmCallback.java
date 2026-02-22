package com.manage_expense.rabbitmq;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RabbitConfirmCallback {

    private final RabbitTemplate rabbitTemplate;

    public RabbitConfirmCallback(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void init() {

        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("Message delivered successfully: {}", correlationData);
            } else {
                log.error("Message delivery failed: {}", cause);
            }
        });

        rabbitTemplate.setReturnsCallback(returned ->
                log.error("Message returned: {}", returned.getMessage()));
    }
}
