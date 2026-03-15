package com.manage_expense.dtos.dto_responses;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private Long categoryId;

    private String categoryName;

    private String description;

    private boolean isDefault;

    private Long parentCategoryId;

    private LocalDateTime createdAt;
}
