package com.manage_expense.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "otp.exchange";
    public static final String SMS_QUEUE = "otp.sms.queue";
    public static final String SMS_VERIFICATION_QUEUE = "otp.sms.verification.queue";
    public static final String EMAIL_QUEUE = "otp.email.queue";
    public static final String SMS_ROUTING_KEY = "otp.sms";
    public static final String SMS_VERIFICATION_ROUTING_KEY = "otp.sms.verification";
    public static final String EMAIL_ROUTING_KEY = "otp.email";

    public static final String DLX = "otp.dlx";
    public static final String SMS_DLQ = "otp.sms.dlq";
    public static final String SMS_VERIFICATION_DLQ = "otp.sms.verification.dlq";
    public static final String EMAIL_DLQ = "otp.email.dlq";

    // Main Exchange
    @Bean
    DirectExchange exchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    // Queues with DLQ support (DLQ -> Dead Letter Queue)
    @Bean
    Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", SMS_DLQ)
                .build();
    }

    @Bean
    Queue smsVerificationQueue() {
        return QueueBuilder.durable(SMS_VERIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", SMS_VERIFICATION_DLQ)
                .build();
    }

    @Bean
    Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ)
                .build();
    }

    // Bindings
    @Bean
    Binding smsBinding(Queue smsQueue, DirectExchange exchange) {
        return BindingBuilder.bind(smsQueue)
                .to(exchange)
                .with(SMS_ROUTING_KEY);
    }

    @Bean
    Binding smsVerificationBinding(Queue smsVerificationQueue, DirectExchange exchange) {
        return BindingBuilder.bind(smsVerificationQueue)
                .to(exchange)
                .with(SMS_VERIFICATION_ROUTING_KEY);
    }

    @Bean
    Binding emailBinding(Queue emailQueue, DirectExchange exchange) {
        return BindingBuilder.bind(emailQueue)
                .to(exchange)
                .with(EMAIL_ROUTING_KEY);
    }

    // Dead Letter Setup
    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue smsDlq() {
        return QueueBuilder.durable(SMS_DLQ).build();
    }

    @Bean
    Queue smsVerificationDlq() {
        return QueueBuilder.durable(SMS_VERIFICATION_DLQ).build();
    }

    @Bean
    Queue emailDlq() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    Binding smsDlqBinding(Queue smsDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(smsDlq)
                .to(deadLetterExchange)
                .with(SMS_DLQ);
    }

    @Bean
    Binding smsVerificationDlqBinding(Queue smsVerificationDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(smsVerificationDlq)
                .to(deadLetterExchange)
                .with(SMS_VERIFICATION_DLQ);
    }

    @Bean
    Binding emailDlqBinding(Queue emailDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDlq)
                .to(deadLetterExchange)
                .with(EMAIL_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}


/*
DirectExchange → routes by exact key
new Queue(..., true) → durable queue
Binding → connects exchange → queue
Routing key decides email vs SMS


Queue → represents a RabbitMQ queue
DirectExchange → represents a direct exchange (routes by exact routing key match)
Binding → links a queue to an exchange
BindingBuilder → fluent API to create bindings cleanly
 */
