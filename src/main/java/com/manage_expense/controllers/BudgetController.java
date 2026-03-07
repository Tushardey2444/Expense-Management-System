package com.manage_expense.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import com.manage_expense.dtos.dto_requests.BudgetCompleteRequest;
import com.manage_expense.dtos.dto_requests.BudgetCreateRequest;
import com.manage_expense.dtos.dto_requests.BudgetUpdateRequest;
import com.manage_expense.dtos.dto_responses.*;
import com.manage_expense.services.services_template.BudgetService;
import com.manage_expense.view.AppViews;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budget")
@Tag(name = "4. Budget API", description = "Endpoints for managing budgets, including creation, updates, completion, retrieval, and deletion")
public class BudgetController {
    @Autowired
    private BudgetService budgetService;

    @PostMapping("/create-budget")
    @JsonView(AppViews.Create.class)
    @Operation(summary = "1. Create Budget", description = "Create a new budget for the authenticated user. Requires budget details in the request body.")
    public ResponseEntity<BudgetResponse> createBudget(Authentication authentication,@Valid @RequestBody BudgetCreateRequest budgetCreateRequest){
        return new ResponseEntity<>(budgetService.createBudget(authentication.getName(),budgetCreateRequest), HttpStatus.CREATED);
    }

    @PutMapping("/update-budget")
    @JsonView(AppViews.Update.class)
    @Operation(summary = "2. Update Budget", description = "Update an existing budget for the authenticated user. Requires updated budget details in the request body.")
    public ResponseEntity<BudgetResponse> updateBudget(Authentication authentication,@Valid @RequestBody BudgetUpdateRequest budgetUpdateRequest){
        return new ResponseEntity<>(budgetService.updateBudget(authentication.getName(),budgetUpdateRequest), HttpStatus.OK);
    }

    @PutMapping("/complete-budget")
    @JsonView(AppViews.Update.class)
    @Operation(summary = "3. Complete Budget", description = "Mark an existing budget as completed for the authenticated user. Requires budget ID and completion details in the request body.")
    public ResponseEntity<BudgetResponse> completeBudget(Authentication authentication,@Valid @RequestBody BudgetCompleteRequest budgetCompleteRequest){
        return new ResponseEntity<>(budgetService.completeBudget(authentication.getName(),budgetCompleteRequest), HttpStatus.OK);
    }

    @DeleteMapping("/delete-budget/{budgetId}")
    @Operation(summary = "4. Delete Budget", description = "Delete an existing budget for the authenticated user. Requires the budget ID as a path variable.")
    public ResponseEntity<ApiResponse> deleteBudget(Authentication authentication, @PathVariable int budgetId){
        return new ResponseEntity<>(budgetService.deleteBudget(authentication.getName(),budgetId), HttpStatus.OK);
    }

    @GetMapping("/get-all-budgets")
    @Operation(summary = "5. Get All Budgets", description = "Retrieve a paginated list of all budgets for the authenticated user. Supports pagination and sorting through query parameters.")
    public ResponseEntity<PageableResponse<BudgetsResponse>> getAllBudgets(Authentication authentication,
                                                         @RequestParam(value = "pageNumber", defaultValue = "0",required = false) int pageNumber,
                                                         @RequestParam(value = "pageSize",defaultValue = "10",required = false) int pageSize,
                                                         @RequestParam(value = "sortBy", defaultValue = "budgetId",required = false) String sortBy,
                                                         @RequestParam(value = "sortDir",defaultValue = "desc",required = false) String sortDir
                                                         ){
        return new ResponseEntity<>(budgetService.getAllBudgets(authentication.getName(), pageNumber, pageSize, sortBy, sortDir), HttpStatus.OK);
    }

    @GetMapping("/get-budget/{budgetId}")
    @Operation(summary = "6. Get Budget by ID", description = "Retrieve details of a specific budget for the authenticated user. Requires the budget ID as a path variable.")
    public ResponseEntity<BudgetResponseDto> getBudget(Authentication authentication, @PathVariable int budgetId){
        return new ResponseEntity<>(budgetService.getBudget(authentication.getName(), budgetId), HttpStatus.OK);
    }

    @DeleteMapping("/delete-multiple-budget")
    @Operation(summary = "7. Delete Multiple Budgets", description = "Delete multiple budgets for the authenticated user. Requires a list of budget IDs in the request body.")
    public ResponseEntity<ApiResponse> deleteMultipleBudget(Authentication authentication, @RequestBody List<Integer> budgetIds){
        return new ResponseEntity<>(budgetService.deleteMultipleBudget(authentication.getName(),budgetIds), HttpStatus.OK);
    }
}