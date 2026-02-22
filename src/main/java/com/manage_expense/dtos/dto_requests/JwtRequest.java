package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;


@Getter
public class JwtRequest {
    @NotBlank(message = "Email cannot be blank !!")
    @Email
    private String email;

    @NotBlank(message = "Password cannot be blank !!")
//    @Pattern(
//            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
//            message = "Password must contain at least 8 characters, one uppercase, one lowercase, one number, and one special character"
//    )
    private String password;

    @NotBlank(message = "Device ID cannot be blank !!")
    private String deviceId;

    private String deviceName;
}
