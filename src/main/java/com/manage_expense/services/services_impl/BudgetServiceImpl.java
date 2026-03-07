package com.manage_expense.services.services_impl;

import com.manage_expense.config.AppConstants;
import com.manage_expense.dtos.dto_requests.BudgetCompleteRequest;
import com.manage_expense.dtos.dto_requests.BudgetCreateRequest;
import com.manage_expense.dtos.dto_requests.BudgetUpdateRequest;
import com.manage_expense.dtos.dto_responses.*;
import com.manage_expense.entities.Budget;
import com.manage_expense.entities.User;
import com.manage_expense.enums.BudgetStatus;
import com.manage_expense.helper.Helper;
import com.manage_expense.repository.BudgetRepository;
import com.manage_expense.repository.UserRepository;
import com.manage_expense.services.services_template.BudgetService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class BudgetServiceImpl implements BudgetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private Helper helper;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    @Override
    public BudgetResponse createBudget(String email, BudgetCreateRequest budgetCreateRequest) {
        User user = userRepository.findBudgetsByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email."));
        if(budgetCreateRequest.getEndDate().isAfter(budgetCreateRequest.getStartDate())) {
            boolean startsInFuture = budgetCreateRequest.getStartDate().isAfter(LocalDate.now());
            boolean isActive = !startsInFuture;
            BudgetStatus budgetStatus = isActive ? BudgetStatus.STARTED : BudgetStatus.QUEUED;
            Budget budget = Budget.builder()
                    .budgetName(budgetCreateRequest.getBudgetName())
                    .amount(budgetCreateRequest.getAmount())
                    .amountSpend(BigDecimal.ZERO)
                    .currency(budgetCreateRequest.getCurrency())
                    .startDate(budgetCreateRequest.getStartDate())
                    .endDate(budgetCreateRequest.getEndDate())
                    .notes(budgetCreateRequest.getNotes())
                    .isActive(isActive)
                    .budgetStatus(budgetStatus)
                    .user(user)
                    .build();
            user.getBudgets().add(budget);
            Budget savedBudget = budgetRepository.save(budget);
            budgetRepository.flush();
            return modelMapper.map(savedBudget, BudgetResponse.class);
        }else{
            throw new IllegalArgumentException("End Date should be greater then Start Date");
        }
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(String email, BudgetUpdateRequest req) {

        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Budget budget = budgetRepository.findBudgetByUser(req.getBudgetId(), user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found, or does not belong to the user"));

        if (!Objects.equals(req.getVersion(), budget.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Budget was modified, please refresh and try again"
            );
        }

        // Hard stop: COMPLETED budgets are immutable
        if (budget.getBudgetStatus() == BudgetStatus.COMPLETED || budget.getBudgetStatus() == BudgetStatus.EXPIRED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Budget cannot be modified");
        }

        LocalDate today = LocalDate.now();

        /* ---------------- Amount ---------------- */
        if (req.getAmount() != null) {
            if (budget.getBudgetStatus() == BudgetStatus.STARTED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Amount cannot be changed after budget has started");
            }

            budget.setAmount(req.getAmount());
        }

        /* ---------------- Start Date ---------------- */
        if (req.getStartDate() != null) {
            if (!budget.getStartDate().isAfter(today)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Start date cannot be changed after budget has started");
            }

            if (req.getStartDate().isEqual(today)) {
                budget.setActive(true);
                budget.setBudgetStatus(BudgetStatus.STARTED);
            }
            budget.setStartDate(req.getStartDate());
        }

        /* ---------------- End Date ---------------- */
        if (req.getEndDate() != null) {

            if (!req.getEndDate().isAfter(budget.getStartDate())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "End date must be after start date");
            }
            budget.setEndDate(req.getEndDate());
        }

        /* ---------------- Notes ---------------- */
        if (req.getNotes() != null) {
            budget.setNotes(req.getNotes());
        }

        if(req.getBudgetName() != null){
            budget.setBudgetName(req.getBudgetName());
        }

        budgetRepository.save(budget);
        budgetRepository.flush();
        return modelMapper.map(budget, BudgetResponse.class);
    }

    @Transactional
    @Override
    public BudgetResponse completeBudget(String email, BudgetCompleteRequest budgetCompleteRequest) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Budget budget = budgetRepository.findBudgetByUser(budgetCompleteRequest.getBudgetId(), user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found, or does not belong to the user"));

        if (!Objects.equals(budgetCompleteRequest.getVersion(), budget.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Budget was modified, please refresh and try again"
            );
        }

        if(budget.getBudgetStatus() != BudgetStatus.QUEUED) {
            if (budget.getBudgetStatus() != BudgetStatus.COMPLETED) {
                budget.setBudgetStatus(BudgetStatus.COMPLETED);
                budget.setActive(false);
                if (budgetCompleteRequest.getNotes() != null) {
                    budget.setNotes(budgetCompleteRequest.getNotes());
                }
                Budget savedBudget = budgetRepository.save(budget);
                budgetRepository.flush();
                return modelMapper.map(savedBudget, BudgetResponse.class);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget has already completed !!");
            }
        }else{
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget has not started, please mark the budget as complete when it will start !!");
        }
    }

    @Override
    public PageableResponse<BudgetsResponse> getAllBudgets(String email, int pageNumber, int pageSize, String sortBy, String sortDir) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Sort sort=(sortDir.equalsIgnoreCase("desc"))?(Sort.by(sortBy).descending()):(Sort.by(sortBy).ascending());
        Pageable pageable= PageRequest.of(pageNumber,pageSize,sort);
        Page<Budget> page=budgetRepository.findBudgetsByUser(user.getUserId(), pageable);
        return helper.getPageableResponse(page, BudgetsResponse.class);
    }

    @Override
    public BudgetResponseDto getBudget(String email, int budgetId) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Budget not found, or does not belong to the user"
                ));

        return modelMapper.map(budget, BudgetResponseDto.class);
    }

    @Transactional
    @Override
    public ApiResponse deleteMultipleBudget(String email, List<Integer> budgetIds) {
        User user = userRepository.findBudgetsByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Remove duplicates and nulls
        List<Integer> ids = budgetIds.stream()
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return ApiResponse.builder()
                    .message("No budget IDs provided.")
                    .success(false)
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        List<Budget> budgets = budgetRepository
                .findAllByIdsAndUserId(ids, user.getUserId());

        if (budgets.isEmpty()) {
            return ApiResponse.builder()
                    .message("No matching budgets found.")
                    .success(false)
                    .status(HttpStatus.NOT_FOUND)
                    .build();
        }

        for (Budget budget : budgets) {
            user.getBudgets().remove(budget);
            budget.setUser(null);
        }

        return ApiResponse.builder()
                .message(budgets.size() + " budget(s) deleted successfully.")
                .success(true)
                .status(HttpStatus.OK)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse deleteBudget(String email, int budgetId) {
        User user = userRepository.findBudgetsByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Budget budget = budgetRepository.findBudgetByUser(budgetId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Budget not found, or does not belong to the user"
                ));

        user.getBudgets().remove(budget);

        return ApiResponse.builder()
                .message("Budget deleted successfully.")
                .success(true)
                .status(HttpStatus.OK)
                .build();
    }

    @Transactional
    @Scheduled(cron = AppConstants.MIDNIGHT_CRON)
    public void changeBudgetStatus(){
        LocalDate today = LocalDate.now();

        List<Budget> budgets = budgetRepository
                .findByBudgetStatusIn(List.of(
                        BudgetStatus.QUEUED,
                        BudgetStatus.STARTED
                ));

        if (budgets.isEmpty()) {
            return;
        }

        for (Budget budget : budgets) {
            if (budget.getBudgetStatus() == BudgetStatus.QUEUED) {
                if (!budget.getStartDate().isAfter(today)) {
                    budget.setBudgetStatus(BudgetStatus.STARTED);
                    budget.setActive(true);
                }
            }
            if (budget.getBudgetStatus() == BudgetStatus.STARTED) {
                if (budget.getEndDate().isBefore(today)) {
                    budget.setBudgetStatus(BudgetStatus.EXPIRED);
                    budget.setActive(false);
                }
            }
        }
        budgetRepository.saveAll(budgets);
    }

    @Transactional
    @Scheduled(cron = AppConstants.MIDNIGHT_CRON)
    public void deleteCompletedBudget() {

        LocalDate today = LocalDate.now();

        List<Budget> budgetsToDelete = budgetRepository
                .findByBudgetStatus(BudgetStatus.COMPLETED)
                .stream()
                .filter(budget ->
                        budget.getEndDate()
                                .plusDays(AppConstants.DELETE_BUDGET)
                                .isBefore(today)
                )
                .toList();

        if (!budgetsToDelete.isEmpty()) {
            budgetRepository.deleteAll(budgetsToDelete);
        }
    }
}