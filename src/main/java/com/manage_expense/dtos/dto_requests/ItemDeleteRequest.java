package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ItemDeleteRequest {
    @NotNull(message = "BudgetId is required!!")
    private Integer budgetId;

    @NotNull(message = "ItemId is required!!")
    private Integer itemId;

    @NotNull(message = "Version is required!!")
    private Long version;
}
