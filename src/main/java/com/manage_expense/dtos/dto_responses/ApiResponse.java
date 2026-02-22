package com.manage_expense.dtos.dto_responses;

import lombok.*;
import org.springframework.http.HttpStatus;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {
    private String message;
    private HttpStatus status;
    private boolean success;
}
