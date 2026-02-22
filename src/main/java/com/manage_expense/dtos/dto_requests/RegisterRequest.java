package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;


@Getter
public class RegisterRequest {
    @NotBlank(message = "Email cannot be blank or null!!")
    @Email(message = "Enter a valid email id!!")
    @NotNull(message = "Email cannot be blank or null !!")
    private String emailId;

    @NotBlank(message = "Password cannot be blank !!")
//    @Pattern(
//            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
//            message = "Password must contain at least 8 characters, one uppercase, one lowercase, one number, and one special character"
//    )
    private String password;

    @NotBlank(message = "Confirm password cannot be blank !!")
//    @Pattern(
//            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
//            message = "Password must contain at least 8 characters, one uppercase, one lowercase, one number, and one special character"
//    )
    private String confirmPassword;
}
