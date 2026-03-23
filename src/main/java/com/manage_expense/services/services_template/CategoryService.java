package com.manage_expense.services.services_template;

import com.manage_expense.dtos.dto_requests.CreateCategoryRequest;
import com.manage_expense.dtos.dto_requests.UpdateCategoryRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.dtos.dto_responses.CategoryResponse;
import com.manage_expense.dtos.dto_responses.ItemsResponseDto;
import com.manage_expense.dtos.dto_responses.PageableResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CategoryService {

    /**
     * Get all categories (default + user categories)
     */
    PageableResponse<CategoryResponse> getParentCategories(String email,
                                                           int pageNumber,
                                                           int pageSize,
                                                           String sortBy,
                                                           String sortDir);

    /**
     * Get subcategories of a parent category
     */
    PageableResponse<CategoryResponse> getSubCategories(String email,
                                                        Long categoryId,
                                                        int pageNumber,
                                                        int pageSize,
                                                        String sortBy,
                                                        String sortDir);

    /**
     * Create category or subcategory
     */
    CategoryResponse createCategory(String email, CreateCategoryRequest request);

    /**
     * Update category or subcategory
     */
    CategoryResponse updateCategory(String email, Long categoryId, UpdateCategoryRequest request);


    ApiResponse updateCategoryIcon(String email, Long categoryId, MultipartFile file) throws IOException;

    /**
     * Delete category or subcategory
     */
    void deleteCategory(String email, Long categoryId);

    PageableResponse<ItemsResponseDto> getAllItemsBySubCategory(String email, int budgetId, Long subCategoryId, int pageNumber, int pageSize, String sortBy, String sortDir);

    /**
     * Create default category or subcategory
     */
    CategoryResponse createDefaultCategory(String email, CreateCategoryRequest request);

    /**
     * Update default category or subcategory
     */
    CategoryResponse updateDefaultCategory(String email, Long categoryId, UpdateCategoryRequest request);

    ApiResponse updateDefaultCategoryIcon(String email, Long categoryId, MultipartFile file) throws IOException;

    /**
     * Delete default category or subcategory
     */
    void deleteDefaultCategory(String email, Long categoryId);


    PageableResponse<String> getSuggestionsOfParentCategoryNameForUser(String email,
                                                                       int pageNumber,
                                                                       int pageSize,
                                                                       String sortBy,
                                                                       String sortDir);

    PageableResponse<String> getSuggestionsOfSubCategoryNameForUser(String email,
                                                                    int pageNumber,
                                                                    int pageSize,
                                                                    String sortBy,
                                                                    String sortDir);
}
