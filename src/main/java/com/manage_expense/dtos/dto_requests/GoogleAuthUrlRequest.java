package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GoogleAuthUrlRequest {
    @NotBlank
    private String deviceId;

    private String deviceName;
}
