package com.manage_expense.helper.OAuth;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OAuthState {
    private String deviceId;
    private String deviceName;
    private long timeStamp;
}
