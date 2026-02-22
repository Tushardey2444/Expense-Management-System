package com.manage_expense.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, DateRangeAware>{
    @Override
    public boolean isValid(
            DateRangeAware dto,
            ConstraintValidatorContext context) {

        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            return true;
        }

        boolean valid = dto.getEndDate().isAfter(dto.getStartDate());

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "End date must be after start date")
                    .addPropertyNode("endDate")
                    .addConstraintViolation();
        }

        return valid;
    }
}
