package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

@Getter
public class VerifyOtpRequest {
    @NotBlank(message = "Email cannot be blank !!")
    @Email
    private String email;

    @NotNull(message = "OTP is required!!")
    @NotBlank(message = "OTP is required!!")
    @Length(min = 6, max = 6, message = "OTP should be 6 digits long!!")
    private String OTP;
}
