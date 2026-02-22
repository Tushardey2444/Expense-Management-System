package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BudgetCompleteRequest {
    @NotNull(message = "BudgetId is required")
    private Integer budgetId;

    @Size(max = 200, message = "Notes must be at most 200 characters")
    private String notes;

    @NotNull(message = "Budget version is required")
    @PositiveOrZero(message = "Budget version must be zero or positive")
    private Long version;
}
