package com.manage_expense.dtos.dto_requests;

import com.manage_expense.enums.Channel;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MailMessageDto implements Serializable {

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please enter valid mobile number!!")
    @Length(min = 10, max = 10, message = "Please enter 10 digit mobile number!!")
    private String toPhoneNumber; // for SMS
    private String email;   // for email
    private String subject;       // for email only
    private String body;            // message content / otp code
    private Channel channel;       // SMS or EMAIL
}
