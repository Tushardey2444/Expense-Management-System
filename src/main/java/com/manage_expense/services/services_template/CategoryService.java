package com.manage_expense.services.services_template;

import com.manage_expense.dtos.dto_requests.CreateCategoryRequest;
import com.manage_expense.dtos.dto_requests.UpdateCategoryRequest;
import com.manage_expense.dtos.dto_responses.CategoryResponse;
import com.manage_expense.dtos.dto_responses.CategoryResponses;
import com.manage_expense.dtos.dto_responses.ItemsResponseDto;
import com.manage_expense.dtos.dto_responses.PageableResponse;

import java.util.List;

public interface CategoryService {

    /**
     * Get all categories (default + user categories)
     */
    List<CategoryResponse> getParentCategories(String email);

    /**
     * Get subcategories of a parent category
     */
    CategoryResponses getSubCategories(String email, Long categoryId);

    /**
     * Create category or subcategory
     */
    CategoryResponse createCategory(String email, CreateCategoryRequest request);

    /**
     * Update category or subcategory
     */
    CategoryResponse updateCategory(String email, Long categoryId, UpdateCategoryRequest request);

    /**
     * Delete category or subcategory
     */
    void deleteCategory(String email, Long categoryId);

    PageableResponse<ItemsResponseDto> getAllItemsBySubCategory(String email, int budgetId, Long subCategoryId, int pageNumber, int pageSize, String sortBy, String sortDir);

    /**
     * Create default category or subcategory
     */
    CategoryResponses createDefaultCategory(String email, CreateCategoryRequest request);

    /**
     * Update default category or subcategory
     */
    CategoryResponses updateDefaultCategory(String email, Long categoryId, UpdateCategoryRequest request);

    /**
     * Delete default category or subcategory
     */
    void deleteDefaultCategory(String email, Long categoryId);


    List<CategoryResponses> getCategories(String email);

}
