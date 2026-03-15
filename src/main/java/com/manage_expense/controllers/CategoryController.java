package com.manage_expense.controllers;

import com.manage_expense.config.AppConstants;
import com.manage_expense.dtos.dto_requests.CreateCategoryRequest;
import com.manage_expense.dtos.dto_requests.UpdateCategoryRequest;
import com.manage_expense.dtos.dto_responses.CategoryResponse;
import com.manage_expense.dtos.dto_responses.CategoryResponses;
import com.manage_expense.dtos.dto_responses.ItemsResponseDto;
import com.manage_expense.dtos.dto_responses.PageableResponse;
import com.manage_expense.services.services_template.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "7. Categories API", description = "Endpoints for managing expense categories, including creation, updates, retrieval, and deletion. This API allows users to organize their expenses into categories and subcategories for better tracking and analysis.")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * Get all categories (default + user categories)
     */

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @GetMapping("/get-categories")
    public ResponseEntity<List<CategoryResponse>> getParentCategories(Authentication authentication) {

        List<CategoryResponse> categories = categoryService.getParentCategories(authentication.getName());
        return ResponseEntity.ok(categories);
    }

    /**
     * Get subcategories of a parent category
     */
    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @GetMapping("/{categoryId}/subcategories")
    public ResponseEntity<CategoryResponses> getSubCategories(
            Authentication authentication,
            @PathVariable Long categoryId) {

        return ResponseEntity.ok(categoryService.getSubCategories(authentication.getName(), categoryId));
    }

    /**
     * Create category or subcategory
     */
    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PostMapping("/create")
    public ResponseEntity<CategoryResponse> createCategory(
            Authentication authentication,
            @Valid @RequestBody CreateCategoryRequest request) {

        CategoryResponse category = categoryService.createCategory(authentication.getName(), request);
        return ResponseEntity.status(201).body(category);
    }

    /**
     * Update category & subcategory
     */
    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PutMapping("/update/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            Authentication authentication,
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {

        CategoryResponse updatedCategory = categoryService.updateCategory(authentication.getName(), categoryId, request);
        return ResponseEntity.ok(updatedCategory);
    }

    /**
     * Delete category & subcategory
     */
    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @DeleteMapping("/delete/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            Authentication authentication,
            @PathVariable Long categoryId) {

        categoryService.deleteCategory(authentication.getName(), categoryId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all items by SubCategory
     */
    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @DeleteMapping("/get-all-items/{budgetId}/{subCategoryId}")
    public ResponseEntity<PageableResponse<ItemsResponseDto>> getAllItemsBySubCategory(
            Authentication authentication,
            @PathVariable int budgetId,
            @PathVariable Long subCategoryId,
            @RequestParam(value = "pageNumber", defaultValue = "0",required = false) int pageNumber,
            @RequestParam(value = "pageSize",defaultValue = "10",required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "createdAt",required = false) String sortBy,
            @RequestParam(value = "sortDir",defaultValue = "desc",required = false) String sortDir) {

        PageableResponse<ItemsResponseDto> items  = categoryService.getAllItemsBySubCategory(authentication.getName(),
                budgetId,
                subCategoryId,
                pageNumber,
                pageSize,
                sortBy,
                sortDir);
        return ResponseEntity.ok(items);
    }

    /**
    * Admin API to create, update, delete default categories
     */

    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @PostMapping("/admin/create-default")
    public ResponseEntity<CategoryResponses> createDefaultCategory(
            Authentication authentication,
            @Valid @RequestBody CreateCategoryRequest request) {

        CategoryResponses category = categoryService.createDefaultCategory(authentication.getName(), request);
        return ResponseEntity.status(201).body(category);
    }

    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @PutMapping("/admin/update-default/{categoryId}")
    public ResponseEntity<CategoryResponses> updateDefaultCategory(
            Authentication authentication,
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryResponses updatedDefaultCategory = categoryService.updateDefaultCategory(authentication.getName(), categoryId, request);
        return ResponseEntity.ok(updatedDefaultCategory);
    }


    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @DeleteMapping("/admin/delete-default/{categoryId}")
    public ResponseEntity<Void> deleteDefaultCategory(
            Authentication authentication,
            @PathVariable Long categoryId) {

        categoryService.deleteDefaultCategory(authentication.getName(), categoryId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @GetMapping("/admin/get-all-categories")
    public ResponseEntity<List<CategoryResponses>> getCategories(Authentication authentication) {

        List<CategoryResponses> categories = categoryService.getCategories(authentication.getName());
        return ResponseEntity.ok(categories);
    }
}
