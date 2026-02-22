package com.manage_expense.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import com.manage_expense.dtos.dto_requests.ItemCreateRequest;
import com.manage_expense.dtos.dto_requests.ItemDeleteRequest;
import com.manage_expense.dtos.dto_requests.ItemUpdateRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.dtos.dto_responses.ItemResponse;
import com.manage_expense.dtos.dto_responses.ItemsResponseDto;
import com.manage_expense.dtos.dto_responses.PageableResponse;
import com.manage_expense.services.services_template.ItemService;
import com.manage_expense.view.AppViews;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item")
public class ItemsController {

    @Autowired
    private ItemService itemService;

    @PostMapping("/create-item")
    @JsonView(AppViews.Create.class)
    public ResponseEntity<ItemResponse> createItem(Authentication authentication, @Valid @RequestBody ItemCreateRequest itemCreateRequest){
        return new ResponseEntity<>(itemService.createItem(authentication.getName(), itemCreateRequest), HttpStatus.CREATED);
    }

    @PutMapping("/update-item")
    @JsonView(AppViews.Update.class)
    public ResponseEntity<ItemResponse> updateItem(Authentication authentication, @Valid @RequestBody ItemUpdateRequest itemUpdateRequest){
        return new ResponseEntity<>(itemService.updateItem(authentication.getName(), itemUpdateRequest), HttpStatus.OK);
    }

    @DeleteMapping("/delete-item")
    @JsonView(AppViews.Delete.class)
    public ResponseEntity<ItemResponse> deleteItem(Authentication authentication, @Valid @RequestBody ItemDeleteRequest itemDeleteRequest){
        return new ResponseEntity<>(itemService.deleteItem(authentication.getName(), itemDeleteRequest), HttpStatus.OK);
    }

    @GetMapping("/get-items-by-budget/{budgetId}")
    public ResponseEntity<PageableResponse<ItemsResponseDto>> getAllItemsByBudget(Authentication authentication,
                                                                          @PathVariable int budgetId,
                                                                          @RequestParam(value = "pageNumber", defaultValue = "0",required = false) int pageNumber,
                                                                          @RequestParam(value = "pageSize",defaultValue = "10",required = false) int pageSize,
                                                                          @RequestParam(value = "sortBy", defaultValue = "budgetId",required = false) String sortBy,
                                                                          @RequestParam(value = "sortDir",defaultValue = "desc",required = false) String sortDir
    ){
        return new ResponseEntity<>(itemService.getAllItemsByBudget(authentication.getName(), budgetId, pageNumber, pageSize, sortBy, sortDir), HttpStatus.OK);
    }

    @DeleteMapping("/delete-multiple-item/{budgetId}")
    @JsonView(AppViews.Delete.class)
    public ResponseEntity<List<ItemResponse>> deleteMultipleItem(Authentication authentication, @RequestBody List<Integer> itemIds, @PathVariable int budgetId){
        return new ResponseEntity<>(itemService.deleteMultipleItems(authentication.getName(), itemIds, budgetId), HttpStatus.OK);
    }
}
