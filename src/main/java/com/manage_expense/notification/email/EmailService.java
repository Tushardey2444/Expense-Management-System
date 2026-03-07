package com.manage_expense.notification.email;

import com.manage_expense.dtos.dto_requests.MailMessageDto;
import com.manage_expense.rabbitmq.RabbitMQConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void sendEmail(MailMessageDto mailMessageDto) throws MessagingException {

        log.info("Sending mail to email: {}, channel: {}, subject: {}",
                mailMessageDto.getEmail(), mailMessageDto.getChannel(), mailMessageDto.getSubject());
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            helper.setTo(mailMessageDto.getEmail());
            helper.setSubject(mailMessageDto.getSubject());
            helper.setText(mailMessageDto.getBody(), true);
            mailSender.send(mimeMessage);
            log.info("Email sent successfully to {}", mailMessageDto.getEmail());

        } catch (Exception e) {
            log.error("Failed to send email to {}",
                    mailMessageDto.getEmail(), e);
            throw e; // Important → So message goes to DLQ
        }
    }
}
