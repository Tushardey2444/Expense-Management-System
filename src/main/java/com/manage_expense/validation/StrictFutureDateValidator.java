package com.manage_expense.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class StrictFutureDateValidator
        implements ConstraintValidator<StrictFutureDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        return value.isAfter(LocalDate.now());
    }
}
