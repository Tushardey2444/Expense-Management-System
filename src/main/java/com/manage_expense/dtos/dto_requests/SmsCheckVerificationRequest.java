package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

@Builder
@Getter
public class SmsCheckVerificationRequest {
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please enter valid mobile number!!")
    @Length(min = 10, max = 10, message = "Please enter 10 digit mobile number!!")
    private String phone;

    @Length(min = 6, max = 6, message = "OTP should be 6 digits long!!")
    private String code;
}
