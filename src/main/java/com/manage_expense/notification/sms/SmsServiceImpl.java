package com.manage_expense.notification.sms;

import com.manage_expense.config.AppConstants;
import com.manage_expense.rabbitmq.RabbitMQConfig;
import com.manage_expense.dtos.dto_requests.MailMessageDto;
import com.manage_expense.dtos.dto_requests.SmsCheckVerificationRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.twilio.exception.ApiException;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Value("${twilio.from-number}")
    private String fromNumber;

    @Value("${twilio.verify-service-sid}")
    private String serviceSid;

    @RabbitListener(queues = RabbitMQConfig.SMS_VERIFICATION_QUEUE)
    @Override
    public void startVerification(MailMessageDto mailMessageDto) {

        log.info("Starting verification for phone number: {}, channel: {}, body: {}",
                mailMessageDto.getToPhoneNumber(), mailMessageDto.getChannel(), mailMessageDto.getBody());

        try{
            Verification.creator(
                    serviceSid,
                    AppConstants.COUNTRY_CODE+mailMessageDto.getToPhoneNumber(),
                    AppConstants.SMSMedium
            ).create();
        } catch (Exception e) {
            log.error("Failed to send verification code to {}",
                    mailMessageDto.getToPhoneNumber(), e);
            throw e; // Important → So message goes to DLQ
        }
    }

    @Override
    public ApiResponse checkVerification(SmsCheckVerificationRequest smsCheckVerificationRequest) {
        try {
            VerificationCheck check = VerificationCheck.creator(serviceSid)
                    .setTo(AppConstants.COUNTRY_CODE + smsCheckVerificationRequest.getPhone())
                    .setCode(smsCheckVerificationRequest.getCode())
                    .create();

            if (AppConstants.APPROVED.equalsIgnoreCase(check.getStatus())) {
                return ApiResponse.builder()
                        .message("Mobile number verified successfully")
                        .status(HttpStatus.OK)
                        .success(true)
                        .build();
            } else {
                return ApiResponse.builder()
                        .message("Invalid or expired verification code")
                        .status(HttpStatus.BAD_REQUEST)
                        .success(false)
                        .build();
            }

        } catch (ApiException e) {
            throw new ApiException("Failed to verify code", e);

        } catch (TwilioException e) {
            throw new ApiException("Twilio service error, please try again later", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SMS_QUEUE)
    @Override
    public void sendMessage(MailMessageDto mailMessageDto){

        log.info("Sending SMS to phone number: {}, channel: {}, body: {}",
                mailMessageDto.getToPhoneNumber(), mailMessageDto.getChannel(), mailMessageDto.getBody());

        try{
            Message.creator(
                    new PhoneNumber(AppConstants.COUNTRY_CODE+mailMessageDto.getToPhoneNumber()),
                    new PhoneNumber(this.fromNumber),
                    mailMessageDto.getBody()
            ).create();
        } catch (Exception e) {
            log.error("Failed to send message to {}",
                    mailMessageDto.getToPhoneNumber(), e);
            throw e; // Important → So message goes to DLQ
        }
    }
}
