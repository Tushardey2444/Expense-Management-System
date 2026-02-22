package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class DeleteUserRequest {
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
