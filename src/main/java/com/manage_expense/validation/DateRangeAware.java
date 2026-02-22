package com.manage_expense.validation;

import java.time.LocalDate;

public interface DateRangeAware {
    LocalDate getStartDate();
    LocalDate getEndDate();
}
