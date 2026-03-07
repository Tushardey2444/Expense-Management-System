package com.manage_expense.dtos.dto_responses;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ItemsResponseDto {
    private int itemId;
    private String itemName;
    private int itemQuantity;
    private BigDecimal price;
    private BigDecimal overallAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
