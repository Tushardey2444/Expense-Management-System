package com.manage_expense.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StrictFutureDateValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrictFutureDate {

    String message() default "Date must be after today";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

