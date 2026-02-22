package com.manage_expense.notification.sms;

import com.manage_expense.dtos.dto_requests.MailMessageDto;
import com.manage_expense.dtos.dto_requests.SmsCheckVerificationRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;

public interface SmsService {
    void startVerification(MailMessageDto mailMessageDto);
    ApiResponse checkVerification(SmsCheckVerificationRequest smsCheckVerificationRequest);
    void sendMessage(MailMessageDto mailMessageDto);
}