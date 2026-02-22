package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ForgetPasswordRequest {
    @NotBlank(message = "Email cannot be blank !!")
    @Email
    private String email;
}
