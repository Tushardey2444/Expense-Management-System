package com.manage_expense.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import com.manage_expense.dtos.dto_requests.BudgetCompleteRequest;
import com.manage_expense.dtos.dto_requests.BudgetCreateRequest;
import com.manage_expense.dtos.dto_requests.BudgetUpdateRequest;
import com.manage_expense.dtos.dto_responses.*;
import com.manage_expense.services.services_template.BudgetService;
import com.manage_expense.view.AppViews;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {
    @Autowired
    private BudgetService budgetService;

    @PostMapping("/create-budget")
    @JsonView(AppViews.Create.class)
    public ResponseEntity<BudgetResponse> createBudget(Authentication authentication,@Valid @RequestBody BudgetCreateRequest budgetCreateRequest){
        return new ResponseEntity<>(budgetService.createBudget(authentication.getName(),budgetCreateRequest), HttpStatus.CREATED);
    }

    @PutMapping("/update-budget")
    @JsonView(AppViews.Update.class)
    public ResponseEntity<BudgetResponse> updateBudget(Authentication authentication,@Valid @RequestBody BudgetUpdateRequest budgetUpdateRequest){
        return new ResponseEntity<>(budgetService.updateBudget(authentication.getName(),budgetUpdateRequest), HttpStatus.OK);
    }

    @PutMapping("/complete-budget")
    @JsonView(AppViews.Update.class)
    public ResponseEntity<BudgetResponse> completeBudget(Authentication authentication,@Valid @RequestBody BudgetCompleteRequest budgetCompleteRequest){
        return new ResponseEntity<>(budgetService.completeBudget(authentication.getName(),budgetCompleteRequest), HttpStatus.OK);
    }

    @DeleteMapping("/delete-budget/{budgetId}")
    public ResponseEntity<ApiResponse> deleteBudget(Authentication authentication, @PathVariable int budgetId){
        return new ResponseEntity<>(budgetService.deleteBudget(authentication.getName(),budgetId), HttpStatus.OK);
    }

    @GetMapping("/get-all-budgets")
    public ResponseEntity<PageableResponse<BudgetsResponse>> getAllBudgets(Authentication authentication,
                                                         @RequestParam(value = "pageNumber", defaultValue = "0",required = false) int pageNumber,
                                                         @RequestParam(value = "pageSize",defaultValue = "10",required = false) int pageSize,
                                                         @RequestParam(value = "sortBy", defaultValue = "budgetId",required = false) String sortBy,
                                                         @RequestParam(value = "sortDir",defaultValue = "desc",required = false) String sortDir
                                                         ){
        return new ResponseEntity<>(budgetService.getAllBudgets(authentication.getName(), pageNumber, pageSize, sortBy, sortDir), HttpStatus.OK);
    }

    @GetMapping("/get-budget/{budgetId}")
    public ResponseEntity<BudgetResponseDto> getBudget(Authentication authentication, @PathVariable int budgetId){
        return new ResponseEntity<>(budgetService.getBudget(authentication.getName(), budgetId), HttpStatus.OK);
    }

    @DeleteMapping("/delete-multiple-budget")
    public ResponseEntity<ApiResponse> deleteMultipleBudget(Authentication authentication, @RequestBody List<Integer> budgetIds){
        return new ResponseEntity<>(budgetService.deleteMultipleBudget(authentication.getName(),budgetIds), HttpStatus.OK);
    }
}