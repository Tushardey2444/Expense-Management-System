package com.manage_expense.rabbitmq;

import com.manage_expense.dtos.dto_requests.MailMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class MailMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public MailMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Send SMS
    public void sendSms(MailMessageDto message) {

        CorrelationData correlationData =
                new CorrelationData(message.getToPhoneNumber() + "-" + UUID.randomUUID());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.SMS_ROUTING_KEY,
                message,
                correlationData
        );

        log.info("SMS sent to exchange: {}", message.getToPhoneNumber());
    }

    // Send SMS
    public void sendVerificationSms(MailMessageDto message) {

        CorrelationData correlationData =
                new CorrelationData(message.getToPhoneNumber() + "-" + UUID.randomUUID());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.SMS_VERIFICATION_ROUTING_KEY,
                message,
                correlationData
        );

        log.info("SMS OTP sent to exchange: {}", message.getToPhoneNumber());
    }

    // Send Email
    public void sendEmail(MailMessageDto message) {

        CorrelationData correlationData =
                new CorrelationData(message.getEmail() + "-" + UUID.randomUUID());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                message,
                correlationData
        );

        log.info("Email sent to exchange: {}", message.getEmail());
    }
}
