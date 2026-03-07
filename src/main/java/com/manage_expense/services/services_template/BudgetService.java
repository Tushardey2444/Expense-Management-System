package com.manage_expense.services.services_template;

import com.manage_expense.dtos.dto_requests.BudgetCompleteRequest;
import com.manage_expense.dtos.dto_requests.BudgetCreateRequest;
import com.manage_expense.dtos.dto_requests.BudgetUpdateRequest;
import com.manage_expense.dtos.dto_responses.*;
import org.springframework.http.HttpHeaders;

import java.util.List;

public interface BudgetService {

    BudgetResponse createBudget(String email, BudgetCreateRequest budgetCreateRequest);

    BudgetResponse updateBudget(String email, BudgetUpdateRequest budgetUpdateRequest);

    ApiResponse deleteBudget(String email, int budgetId);

    BudgetResponse completeBudget(String email, BudgetCompleteRequest budgetCompleteRequest);

    PageableResponse<BudgetsResponse> getAllBudgets(String email, int pageNumber, int pageSize, String sortBy, String sortDir);

    BudgetResponseDto getBudget(String email, int budgetId);

    ApiResponse deleteMultipleBudget(String email, List<Integer> budgetIds);
}