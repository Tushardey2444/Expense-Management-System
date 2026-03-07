package com.manage_expense.dtos.dto_requests;

import com.manage_expense.validation.DateRangeAware;
import com.manage_expense.validation.StrictFutureDate;
import com.manage_expense.validation.ValidDateRange;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@ValidDateRange
public class BudgetUpdateRequest implements DateRangeAware {

    @NotNull(message = "BudgetId is required")
    private Integer budgetId;

    @Size(max = 50, message = "Budget name must be at most 50 characters")
    private String budgetName;

    @Digits(integer = 19, fraction = 2, message = "Amount format is invalid")
    @DecimalMin(value = "0.00", message = "Amount must be a positive value or zero")
    private BigDecimal amount;

    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @StrictFutureDate(message = "End date must be a future date")
    private LocalDate endDate;

    @Size(max = 200, message = "Notes must be at most 200 characters")
    private String notes;

    @NotNull(message = "Budget version is required")
    @PositiveOrZero(message = "Budget version must be zero or positive")
    private Long version;
}
