package com.manage_expense.services.services_template;

import com.manage_expense.dtos.dto_requests.ItemCreateRequest;
import com.manage_expense.dtos.dto_requests.ItemDeleteRequest;
import com.manage_expense.dtos.dto_requests.ItemUpdateRequest;
import com.manage_expense.dtos.dto_responses.ItemResponse;
import com.manage_expense.dtos.dto_responses.ItemsResponseDto;
import com.manage_expense.dtos.dto_responses.PageableResponse;

import java.util.List;

public interface ItemService {
    ItemResponse createItem(String email, ItemCreateRequest itemCreateRequest);

    ItemResponse updateItem(String email, ItemUpdateRequest itemUpdateRequest);

    ItemResponse deleteItem(String email, ItemDeleteRequest itemDeleteRequest);

    PageableResponse<ItemsResponseDto> getAllItemsByBudget(String email, int budgetId, int pageNumber, int pageSize, String sortBy, String sortDir);

    List<ItemResponse> deleteMultipleItems(String email, List<Integer> itemIds, int budgetId);
}
