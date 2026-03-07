package com.manage_expense.dtos.dto_responses;

import com.fasterxml.jackson.annotation.JsonView;
import com.manage_expense.enums.BudgetStatus;
import com.manage_expense.enums.Currency;
import com.manage_expense.view.AppViews;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BudgetResponse {
    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private int budgetId;

    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private String budgetName;

    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private long version;

    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private BigDecimal amount;

    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private BigDecimal amountSpend;

    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private Currency currency;

    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private LocalDate startDate;

    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private LocalDate endDate;

    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private String notes;

    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private BudgetStatus budgetStatus;

    @JsonView({AppViews.Create.class, AppViews.Update.class})
    private boolean isActive;
}
