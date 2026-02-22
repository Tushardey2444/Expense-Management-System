package com.manage_expense.dtos.dto_responses;

import com.fasterxml.jackson.annotation.JsonView;
import com.manage_expense.view.AppViews;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ItemResponse {
    @JsonView({AppViews.Create.class,AppViews.Update.class,AppViews.Delete.class})
    private int itemId;

    @JsonView({AppViews.Create.class,AppViews.Update.class,AppViews.Delete.class})
    private int budgetId;

    @JsonView({AppViews.Create.class,AppViews.Update.class,AppViews.Delete.class})
    private long version;

    @JsonView({AppViews.Create.class,AppViews.Update.class,AppViews.Delete.class})
    private BigDecimal amountSpend;

    @JsonView({AppViews.Create.class,AppViews.Update.class,AppViews.Delete.class})
    private String itemName;

    @JsonView({AppViews.Create.class,AppViews.Update.class})
    private int itemQuantity;

    @JsonView({AppViews.Create.class,AppViews.Update.class})
    private int price;

    @JsonView({AppViews.Create.class,AppViews.Update.class,AppViews.Delete.class})
    private BigDecimal overallAmount;

    @JsonView({AppViews.Create.class,AppViews.Update.class})
    private LocalDateTime createdAt;

    @JsonView({AppViews.Create.class,AppViews.Update.class})
    private LocalDateTime updatedAt;
}
