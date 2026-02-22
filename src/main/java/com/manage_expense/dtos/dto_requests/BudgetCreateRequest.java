package com.manage_expense.dtos.dto_requests;

import com.manage_expense.enums.Currency;
import com.manage_expense.validation.DateRangeAware;
import com.manage_expense.validation.StrictFutureDate;
import com.manage_expense.validation.ValidDateRange;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@ValidDateRange
public class BudgetCreateRequest implements DateRangeAware {

    @NotNull(message = "Budget name is required")
    @Size(max = 50, message = "Budget name must be at most 50 characters")
    private String budgetName;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", message = "Amount must be a positive value or zero")
    @Digits(integer = 19, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @StrictFutureDate(message = "End date must be a future date")
    private LocalDate endDate;

    @Size(max = 200, message = "Notes must be at most 200 characters")
    private String notes;

}
