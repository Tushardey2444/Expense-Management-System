package com.manage_expense.controllers;

import com.manage_expense.dtos.dto_requests.MailMessageDto;
import com.manage_expense.notification.sms.SmsService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/twilio")
@Hidden
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/test/send")
    public void sendVerification(@RequestParam MailMessageDto mailMessageDto) {
        smsService.startVerification(mailMessageDto);
    }

    @PostMapping("/message")
    public String sendMessage(@RequestParam Map<String,String> body){
        body.forEach((k,v) -> System.out.println(k + " : " + v));
        return "Message sent successfully!!";
    }
}
