package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshTokenRequest {
    @NotBlank(message = "Device ID cannot be blank !!")
    private String deviceId;

    private String deviceName;
}
