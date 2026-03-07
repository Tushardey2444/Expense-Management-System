package com.manage_expense.exceptions;

public class PublishingException extends RuntimeException{
    public PublishingException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
