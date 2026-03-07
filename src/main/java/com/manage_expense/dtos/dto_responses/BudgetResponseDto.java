package com.manage_expense.dtos.dto_responses;

import com.manage_expense.enums.BudgetStatus;
import com.manage_expense.enums.Currency;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BudgetResponseDto {
    private int budgetId;
    private String budgetName;
    private BigDecimal amount;
    private BigDecimal amountSpend;
    private Currency currency;
    private LocalDate startDate;
    private LocalDate endDate;
    private BudgetStatus budgetStatus;
    private String notes;
    private boolean isActive;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
