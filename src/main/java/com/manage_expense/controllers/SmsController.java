package com.manage_expense.controllers;

import com.manage_expense.dtos.dto_requests.MailMessageDto;
import com.manage_expense.notification.sms.SmsService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/api/sms")
@Hidden
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/send")
    public void sendVerification(@RequestParam MailMessageDto mailMessageDto) {
        smsService.startVerification(mailMessageDto);
    }
}
