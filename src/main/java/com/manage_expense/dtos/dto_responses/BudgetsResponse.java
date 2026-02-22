package com.manage_expense.dtos.dto_responses;

import com.manage_expense.enums.BudgetStatus;
import com.manage_expense.enums.Currency;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BudgetsResponse {
    private int budgetId;
    private String budgetName;
//    private BigDecimal amount;
//    private BigDecimal amountSpend;
//    private Currency currency;
    private LocalDate startDate;
//    private LocalDate endDate;
//    private String notes;
    private BudgetStatus budgetStatus;
    private boolean isActive;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
    private long version;
}
