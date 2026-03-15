package com.manage_expense.dtos.dto_requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
    private String categoryName;

    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    private Long parentCategoryId;
}
