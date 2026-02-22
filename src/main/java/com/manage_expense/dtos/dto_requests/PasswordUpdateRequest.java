package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordUpdateRequest {
    @Email(message = "Email cannot be blank !!")
    @NotBlank
    private String email;
    @NotBlank(message = "Password cannot be blank !!")
    private String newPassword;
    @NotBlank(message = "Confirm password cannot be blank !!")
    private String confirmPassword;
}
