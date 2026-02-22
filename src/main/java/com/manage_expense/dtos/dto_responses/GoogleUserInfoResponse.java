package com.manage_expense.dtos.dto_responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleUserInfoResponse {
    private String sub;
    private String email;
    private Boolean email_verified;
    private String name;
    private String given_name;
    private String family_name;
    private String picture;
    private String locale;
}
