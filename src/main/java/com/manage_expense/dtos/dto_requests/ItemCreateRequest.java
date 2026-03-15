package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ItemCreateRequest {
    @NotNull(message = "BudgetId is required!!")
    private Integer budgetId;

    @NotNull(message = "SubCategoryId is required!!")
    private Long subCategoryId;

    @NotNull(message = "Item name is required!!")
    @Size(max = 100, message = "Item name must be at most 100 characters")
    private String itemName;

    @NotNull(message = "version is required!!")
    private Long version;

    @NotNull(message = "Price is required!!")
    @DecimalMin(value = "1.00", message = "Price cannot be negative or zero")
    private BigDecimal price;
}
