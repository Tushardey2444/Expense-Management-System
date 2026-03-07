package com.manage_expense.exceptions;

public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String msg) {
        super(msg);
    }
}
