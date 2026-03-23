package com.manage_expense.controllers;

import com.manage_expense.config.AppConstants;
import com.manage_expense.dtos.dto_requests.CreateCategoryRequest;
import com.manage_expense.dtos.dto_requests.UpdateCategoryRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.dtos.dto_responses.CategoryResponse;
import com.manage_expense.dtos.dto_responses.ItemsResponseDto;
import com.manage_expense.dtos.dto_responses.PageableResponse;
import com.manage_expense.services.services_template.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


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
    @GetMapping("/get-parent-categories")
    @Operation(summary = "6. Get default / custom categories", description = "Get default / custom categories for the authenticated admin / user.")
    public ResponseEntity<PageableResponse<CategoryResponse>> getParentCategories(Authentication authentication,
                                                                      @RequestParam(value = "pageNumber", defaultValue = "0",required = false) int pageNumber,
                                                                      @RequestParam(value = "pageSize",defaultValue = "10",required = false) int pageSize,
                                                                      @RequestParam(value = "sortBy", defaultValue = "categoryId",required = false) String sortBy,
                                                                      @RequestParam(value = "sortDir",defaultValue = "desc",required = false) String sortDir
                                                                      ) {

        PageableResponse<CategoryResponse> categories = categoryService.getParentCategories(authentication.getName(), pageNumber, pageSize, sortBy, sortDir);
        return ResponseEntity.ok(categories);
    }

    /**
     * Get subcategories of a parent category
     */
    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @GetMapping("/{categoryId}/get-subcategories")
    @Operation(summary = "7. Get default / custom subCategories", description = "Get default / custom subCategories for the authenticated admin / user.")
    public ResponseEntity<PageableResponse<CategoryResponse>> getSubCategories(
            Authentication authentication,
            @PathVariable Long categoryId,
            @RequestParam(value = "pageNumber", defaultValue = "0",required = false) int pageNumber,
            @RequestParam(value = "pageSize",defaultValue = "10",required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "categoryId",required = false) String sortBy,
            @RequestParam(value = "sortDir",defaultValue = "desc",required = false) String sortDir) {

        return ResponseEntity.ok(categoryService.getSubCategories(authentication.getName(), categoryId, pageNumber, pageSize, sortBy, sortDir));
    }

    /**
     * Create category or subcategory
     */
    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PostMapping("/create")
    @Operation(summary = "1. Create custom category / subCategory", description = "Create a custom new category / subCategory by authenticated admin / user. Requires category / subCategory details in the request body.")
    public ResponseEntity<CategoryResponse> createCategory(
            Authentication authentication,
            @Valid @RequestBody CreateCategoryRequest request) {

        CategoryResponse category = categoryService.createCategory(authentication.getName(), request);
        return ResponseEntity.status(201).body(category);
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PutMapping(value = "/update-custom-icon/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "12. Update custom category / subCategory icon", description = "Update a custom category / subCategory icon by authenticated admin / user. Requires categoryId / subCategoryId in the path variable and the file in the request param.")
    public ResponseEntity<ApiResponse> updateCategoryIcon(Authentication authentication, @PathVariable Long categoryId, @RequestParam("file") MultipartFile file) throws IOException{
        return ResponseEntity.ok(categoryService.updateCategoryIcon(authentication.getName(), categoryId, file));
    }

    /**
     * Update category & subcategory
     */
    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PutMapping("/update/{categoryId}")
    @Operation(summary = "2. Update custom category / subCategory", description = "Update a custom category / subCategory by authenticated admin / user. Requires category / subCategory details in the request body.")
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
    @Operation(summary = "3. Delete custom category / subCategory", description = "Delete a custom category / subCategory by authenticated admin / user.")
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
    @Operation(summary = "8. Get items by subCategories", description = "Get all items by subCategories page wise for the authenticated admin / user.")
    public ResponseEntity<PageableResponse<ItemsResponseDto>> getAllItemsBySubCategory(
            Authentication authentication,
            @PathVariable int budgetId,
            @PathVariable Long subCategoryId,
            @RequestParam(value = "pageNumber", defaultValue = "0",required = false) int pageNumber,
            @RequestParam(value = "pageSize",defaultValue = "10",required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "categoryId",required = false) String sortBy,
            @RequestParam(value = "sortDir",defaultValue = "desc",required = false) String sortDir) {

        PageableResponse<ItemsResponseDto> items  = categoryService.getAllItemsBySubCategory(
                authentication.getName(),
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
    @Operation(summary = "9. Create default category / subCategory", description = "Create a default new category / subCategory by authenticated admin. Requires category / subCategory details in the request body.")
    public ResponseEntity<CategoryResponse> createDefaultCategory(
            Authentication authentication,
            @Valid @RequestBody CreateCategoryRequest request) {

        CategoryResponse category = categoryService.createDefaultCategory(authentication.getName(), request);
        return ResponseEntity.status(201).body(category);
    }

    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @PutMapping(value = "/admin/update-default-icon/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "12. Update default category / subCategory icon", description = "Update a default category / subCategory icon by authenticated admin. Requires categoryId / subCategoryId in the path variable and the file in the request param.")
    public ResponseEntity<ApiResponse> updateDefaultCategoryIcon(Authentication authentication, @PathVariable Long categoryId, @RequestParam("file") MultipartFile file) throws IOException{
        return ResponseEntity.ok(categoryService.updateDefaultCategoryIcon(authentication.getName(), categoryId, file));
    }

    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @PutMapping("/admin/update-default/{categoryId}")
    @Operation(summary = "10. Update default category / subCategory", description = "Update a default category / subCategory by authenticated admin. Requires category / subCategory details in the request body.")
    public ResponseEntity<CategoryResponse> updateDefaultCategory(
            Authentication authentication,
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryResponse updatedDefaultCategory = categoryService.updateDefaultCategory(authentication.getName(), categoryId, request);
        return ResponseEntity.ok(updatedDefaultCategory);
    }


    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @DeleteMapping("/admin/delete-default/{categoryId}")
    @Operation(summary = "11. Delete default category / subCategory", description = "Delete a default category / subCategory by authenticated admin.")
    public ResponseEntity<Void> deleteDefaultCategory(
            Authentication authentication,
            @PathVariable Long categoryId) {

        categoryService.deleteDefaultCategory(authentication.getName(), categoryId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @GetMapping("/get-category-suggestions")
    @Operation(summary = "4. Get category suggestions", description = "Get category suggestions for the authenticated admin / user.")
    public ResponseEntity<PageableResponse<String>> getSuggestionsOfParentCategoryNameForUser(Authentication authentication,
                                                                                              @RequestParam(value = "pageNumber", defaultValue = "0",required = false) int pageNumber,
                                                                                              @RequestParam(value = "pageSize",defaultValue = "10",required = false) int pageSize,
                                                                                              @RequestParam(value = "sortBy", defaultValue = "categoryName",required = false) String sortBy,
                                                                                              @RequestParam(value = "sortDir",defaultValue = "asc",required = false) String sortDir){
        return ResponseEntity.ok(categoryService.getSuggestionsOfParentCategoryNameForUser(
                authentication.getName(),
                pageNumber,
                pageSize,
                sortBy,
                sortDir
                ));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @GetMapping("/get-subcategory-suggestions")
    @Operation(summary = "5. Get subCategory suggestions", description = "Get subCategory suggestions for the authenticated admin / user.")
    public ResponseEntity<PageableResponse<String>> getSuggestionsOfSubCategoryNameForUser(Authentication authentication,
                                                                                              @RequestParam(value = "pageNumber", defaultValue = "0",required = false) int pageNumber,
                                                                                              @RequestParam(value = "pageSize",defaultValue = "10",required = false) int pageSize,
                                                                                              @RequestParam(value = "sortBy", defaultValue = "categoryName",required = false) String sortBy,
                                                                                              @RequestParam(value = "sortDir",defaultValue = "asc",required = false) String sortDir){
        return ResponseEntity.ok(categoryService.getSuggestionsOfSubCategoryNameForUser(
                authentication.getName(),
                pageNumber,
                pageSize,
                sortBy,
                sortDir
        ));
    }
}
