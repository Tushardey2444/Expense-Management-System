package com.manage_expense.dtos.dto_responses;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSessionResponse {

    private String sessionId;
    private String deviceName;
    private Instant createdAt;
}
