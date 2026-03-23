package com.manage_expense.controllers;

import com.manage_expense.dtos.dto_requests.MailMessageDto;
import com.manage_expense.notification.sms.SmsService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/twilio")
@Hidden
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/test-send")
    public void sendVerification(@RequestParam MailMessageDto mailMessageDto) {
        smsService.startVerification(mailMessageDto);
    }

    @PostMapping(value = "/message", produces = MediaType.APPLICATION_XML_VALUE)
    public String sendMessage(@RequestParam Map<String, String> body) {

        String incomingMsg = body.get("Body").trim().toLowerCase();
        String from = body.get("From");

        String response;

        switch (incomingMsg) {

            case "hi":
                response = """
                    Hello 👋
                    
                    What would you like to do?
                    
                    1. Create Budget
                    2. Create Item
                    3. Update Budget
                    4. Update Item
                    5. Delete Budget
                    6. Delete Item
                    """;
                break;

            case "create budget":
            case "1":
                response = """
                    Let's create a budget 💰
                    
                    Please select a category:
                    
                    1. Food
                    2. Transport
                    3. Entertainment
                    4. Custom
                    
                    Reply with the category number.
                    """;
                break;

            case "custom":
                response = """
                    Please provide the custom category name.
                    """;
                break;

            case "create item":
            case "2":
                response = """
                    Creating an item 🛒
                    
                    Select category:
                    
                    1. Food
                    2. Transport
                    3. Custom
                    
                    Reply with category number.
                    """;
                break;

            case "cancel":
                response = """
                    Operation cancelled.
                    
                    Type *hi* to start again.
                    """;
                break;

            default:
                response = """
                    Sorry, I didn't understand that.
                    
                    Type *hi* to start.
                    """;
        }

        return """
            <Response>
                <Message>%s</Message>
            </Response>
            """.formatted(response);
    }
}
