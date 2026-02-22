package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ItemUpdateRequest {
    @NotNull(message = "BudgetId is required!!")
    private Integer budgetId;

    @NotNull(message = "ItemId is required!!")
    private Integer itemId;

    @Size(max = 100, message = "Item name must be at most 100 characters")
    private String itemName;

    private Integer itemQuantity;

    @NotNull(message = "Version is required!!")
    private Long version;

    private BigDecimal price;
}
