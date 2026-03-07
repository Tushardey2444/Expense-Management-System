package com.manage_expense.exceptions;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message, Throwable ex) {
        super(message, ex);
    }
}
