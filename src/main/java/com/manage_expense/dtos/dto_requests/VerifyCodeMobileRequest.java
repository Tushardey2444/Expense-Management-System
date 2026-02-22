package com.manage_expense.dtos.dto_requests;

import lombok.Getter;
import org.hibernate.validator.constraints.Length;

@Getter
public class VerifyCodeMobileRequest {

    @Length(min = 6, max = 6, message = "OTP should be 6 digits long!!")
    private String code;
}
