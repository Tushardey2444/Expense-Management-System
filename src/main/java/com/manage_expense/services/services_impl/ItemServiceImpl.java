package com.manage_expense.services.services_impl;

import com.manage_expense.dtos.dto_requests.ItemCreateRequest;
import com.manage_expense.dtos.dto_requests.ItemDeleteRequest;
import com.manage_expense.dtos.dto_requests.ItemUpdateRequest;
import com.manage_expense.dtos.dto_responses.*;
import com.manage_expense.entities.Budget;
import com.manage_expense.entities.Items;
import com.manage_expense.entities.User;
import com.manage_expense.enums.BudgetStatus;
import com.manage_expense.helper.Helper;
import com.manage_expense.repository.BudgetRepository;
import com.manage_expense.repository.ItemRepository;
import com.manage_expense.repository.UserRepository;
import com.manage_expense.services.services_template.ItemService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private Helper helper;

    @Transactional
    @Override
    public ItemResponse createItem(String email, ItemCreateRequest req) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Budget budget = budgetRepository.findBudgetByUser(req.getBudgetId(), user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found"));

        // Optimistic lock check
        if (!Objects.equals(budget.getVersion(), req.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Budget was modified on another device. Please refresh."
            );
        }

        // Status validation
        if (budget.getBudgetStatus() != BudgetStatus.STARTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot add items to a budget with this status"
            );
        }

        // Create item
        Items item = Items.builder()
                .itemName(req.getItemName())
                .itemQuantity(req.getItemQuantity())
                .price(req.getPrice())
                .budget(budget)
                .build();

        BigDecimal itemTotal = item.getTotalAmount();

        budget.getItems().add(item);
        budget.setAmountSpend(budget.getAmountSpend().add(itemTotal));

        Items savedItem = itemRepository.save(item);
        budgetRepository.flush();

        // Map response AFTER version increment
        ItemResponse response = modelMapper.map(savedItem, ItemResponse.class);
        response.setBudgetId(budget.getBudgetId());
        response.setAmountSpend(budget.getAmountSpend());
        response.setOverallAmount(itemTotal);
        response.setVersion(budget.getVersion());

        return response;
    }

    @Transactional
    @Override
    public ItemResponse updateItem(String email, ItemUpdateRequest req) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Budget budget = budgetRepository.findBudgetByUser(req.getBudgetId(), user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found"));

        Items item = itemRepository.findItemByBudget(req.getItemId(), budget.getBudgetId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found"));

        // Optimistic lock check (ALWAYS)
        if (!Objects.equals(req.getVersion(), budget.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Budget was modified on another device. Please refresh."
            );
        }

        // Save original total
        BigDecimal oldTotal = item.getTotalAmount();

        /* ----------- Updates ----------- */

        if (req.getItemName() != null) {
            item.setItemName(req.getItemName());
        }

        if (req.getItemQuantity() != null) {
            if (req.getItemQuantity() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Quantity must be greater than 0");
            }
            item.setItemQuantity(req.getItemQuantity());
        }


        if (req.getPrice() != null) {
            if (req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Price must be greater than 0");
            }
            item.setPrice(req.getPrice());
        }

        // Save item
        Items savedItem = itemRepository.save(item);

        // Recalculate budget spend
        BigDecimal newTotal = savedItem.getTotalAmount();
        budget.setAmountSpend(
                budget.getAmountSpend().subtract(oldTotal).add(newTotal)
        );

        // IMPORTANT: save budget to increment version
        Budget savedBudget = budgetRepository.save(budget);

        budgetRepository.flush();

        // Response
        ItemResponse response = modelMapper.map(savedItem, ItemResponse.class);
        response.setBudgetId(savedBudget.getBudgetId());
        response.setAmountSpend(savedBudget.getAmountSpend());
        response.setOverallAmount(newTotal);
        response.setVersion(savedBudget.getVersion());

        return response;
    }

    @Transactional
    @Override
    public ItemResponse deleteItem(String email, ItemDeleteRequest req) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Budget budget = budgetRepository.findBudgetByUser(req.getBudgetId(), user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found"));

        Items item = itemRepository.findItemByBudget(req.getItemId(), budget.getBudgetId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found"));

        // Optimistic lock check (ALWAYS)
        if (!Objects.equals(req.getVersion(), budget.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Budget was modified on another device. Please refresh."
            );
        }

        BigDecimal overallAmount = item.getTotalAmount();
        itemRepository.deleteById(item.getItemId());

        budget.setAmountSpend(
                budget.getAmountSpend().subtract(overallAmount)
        );

        // IMPORTANT: save budget to increment version
        Budget savedBudget = budgetRepository.save(budget);
        budgetRepository.flush();

        ItemResponse response = modelMapper.map(item, ItemResponse.class);
        response.setBudgetId(savedBudget.getBudgetId());
        response.setAmountSpend(savedBudget.getAmountSpend());
        response.setOverallAmount(overallAmount);
        response.setVersion(savedBudget.getVersion());

        return response;
    }

    @Transactional(readOnly = true)
    @Override
    public PageableResponse<ItemsResponseDto> getAllItemsByBudget(String email, int budgetId, int pageNumber, int pageSize, String sortBy, String sortDir) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // fetch budget to check ownership and existence
        Budget budget = budgetRepository.findBudgetByUser(budgetId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found"));

        Sort sort=(sortDir.equalsIgnoreCase("desc"))?(Sort.by(sortBy).descending()):(Sort.by(sortBy).ascending());
        Pageable pageable = PageRequest.of(pageNumber,pageSize,sort);
        Page<Items> pageItems=itemRepository.findItemsByBudget(budget.getBudgetId(), pageable);
        PageableResponse<ItemsResponseDto> pageableResponse =  helper.getPageableResponse(pageItems, ItemsResponseDto.class);
        List<ItemsResponseDto> itemsResponseDtos = pageableResponse.getContent();
        for (ItemsResponseDto itemsResponseDto: itemsResponseDtos) {
            itemsResponseDto.setOverallAmount(itemsResponseDto.getPrice().multiply(BigDecimal.valueOf(itemsResponseDto.getItemQuantity())));
        }
        return pageableResponse;
    }

    @Transactional
    @Override
    public List<ItemResponse> deleteMultipleItems(String email, List<Integer> itemIds, int budgetId) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // fetch budget to check ownership and existence
        Budget budget = budgetRepository.findBudgetByUser(budgetId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found"));

        // Remove duplicates and nulls
        List<Integer> ids = itemIds.stream()
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No item IDs provided"
            );
        }

        List<Items> itemsToDelete = itemRepository.findAllByIdsAndBudgetId(ids, budget.getBudgetId());

        if (itemsToDelete.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No matching items found"
            );
        }

        BigDecimal totalAmountToSubtract = itemsToDelete.stream()
                .map(Items::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        itemRepository.deleteAll(itemsToDelete);

        BigDecimal currentSpent = budget.getAmountSpend() != null
                ? budget.getAmountSpend()
                : BigDecimal.ZERO;

        BigDecimal newAmount = currentSpent.subtract(totalAmountToSubtract);

        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            newAmount = BigDecimal.ZERO;
        }

        budget.setAmountSpend(newAmount);

        // IMPORTANT: save budget to increment version
        Budget savedBudget = budgetRepository.save(budget);
        budgetRepository.flush();

        return itemsToDelete.stream()
                .map(item -> {
                    ItemResponse response = modelMapper.map(item, ItemResponse.class);
                    response.setBudgetId(savedBudget.getBudgetId());
                    response.setAmountSpend(savedBudget.getAmountSpend());
                    response.setOverallAmount(item.getTotalAmount());
                    response.setVersion(savedBudget.getVersion());
                    return response;
                })
                .toList();
    }
}