package com.manage_expense.services.services_impl;

import com.manage_expense.dtos.dto_requests.CreateCategoryRequest;
import com.manage_expense.dtos.dto_requests.UpdateCategoryRequest;
import com.manage_expense.dtos.dto_responses.CategoryResponse;
import com.manage_expense.dtos.dto_responses.CategoryResponses;
import com.manage_expense.dtos.dto_responses.ItemsResponseDto;
import com.manage_expense.dtos.dto_responses.PageableResponse;
import com.manage_expense.entities.Budget;
import com.manage_expense.entities.Category;
import com.manage_expense.entities.Items;
import com.manage_expense.entities.User;
import com.manage_expense.helper.Helper;
import com.manage_expense.repository.BudgetRepository;
import com.manage_expense.repository.CategoryRepository;
import com.manage_expense.repository.ItemRepository;
import com.manage_expense.repository.UserRepository;
import com.manage_expense.services.services_template.CategoryService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private Helper helper;

    public CategoryResponses categoryToCategoryResponses(Category category) {

        return CategoryResponses.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .isDefault(category.isDefault())
                .parentCategoryId(
                        category.getParentCategory() != null
                                ? category.getParentCategory().getCategoryId()
                                : null
                )
                .createdAt(category.getCreatedAt())
                .subCategories(
                        category.getSubCategories()
                                .stream()
                                .map(this::categoryToCategoryResponses)
                                .collect(Collectors.toSet())
                )
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getParentCategories(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        List<Category> parentCategories = categoryRepository.findParentCategoriesForUser(user.getUserId());
        List<CategoryResponse> categoryResponses = new ArrayList<>();

        for(Category category: parentCategories){
            CategoryResponse categoryResponse =  modelMapper.map(category, CategoryResponse.class);
            categoryResponses.add(categoryResponse);
        }

        return categoryResponses;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponses getSubCategories(String email, Long categoryId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        Category category = categoryRepository.findDefaultOrCustomParentCategoryWithSubCategoriesByUserId(categoryId, user.getUserId()).orElseThrow(() -> new IllegalStateException("Parent category not found for current user!!"));

        CategoryResponses categoryResponses = categoryToCategoryResponses(category);
        categoryResponses.setParentCategoryId(null);
        return categoryResponses;
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(String email, CreateCategoryRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        String categoryName  = request.getCategoryName().trim();
        Category category = Category.builder()
                .categoryName(categoryName)
                .isDefault(false)
                .description(request.getDescription())
                .parentCategory(null)
                .user(user)
                .build();

        Long parentCategoryId = null;
        if(request.getParentCategoryId() == null){
            if(categoryRepository.existsByCategoryNameAndUser_UserIdAndParentCategoryIsNull(categoryName, user.getUserId())){
                throw new IllegalStateException("Custom parent category already exists with provided name");
            }

            if(categoryRepository.existsByCategoryNameAndUserIsNullAndParentCategoryIsNull(categoryName)){
                throw new IllegalStateException("Default parent category already exists with provided name");
            }

        }else{
            Category parentCategory = categoryRepository
                    .findCustomParentCategoryWithSubCategoriesByUserId(request.getParentCategoryId(), user.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Custom parent category not found with provided parent categoryId !!"));

            if(categoryRepository.existsByCategoryNameAndUser_UserIdAndParentCategoryCategoryId(categoryName, user.getUserId(), parentCategory.getCategoryId())){
                throw new IllegalStateException("Custom sub category already exists with provided name under provided parent category !!");
            }

            parentCategoryId = parentCategory.getCategoryId();
            category.setParentCategory(parentCategory);
            parentCategory.getSubCategories().add(category);
        }

        user.getCategories().add(category);

        Category savedCategory = categoryRepository.save(category);
        categoryRepository.flush();
        CategoryResponse categoryResponse = modelMapper.map(savedCategory, CategoryResponse.class);
        categoryResponse.setParentCategoryId(parentCategoryId);

        return categoryResponse;
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(String email, Long categoryId, UpdateCategoryRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        Category category = categoryRepository.findCustomCategoryOrSubCategory(categoryId, user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Custom category not found with provided categoryId !!"));

        String newCategoryName = request.getCategoryName().trim();

        if (category.getParentCategory() == null) {

            if (request.getParentCategoryId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided categoryId is a parent category, parent category can't be change to sub category");
            }

            if (!category.getCategoryName().equalsIgnoreCase(newCategoryName)) {
                Optional<Category> existCategory = categoryRepository.findParentCategoryByNameForUserOrDefault(
                        newCategoryName,
                        user.getUserId()
                );

                if (existCategory.isPresent()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default / Custom parent category already exist with same category name");
                }

                category.setCategoryName(newCategoryName);
            }

        } else {
            if (request.getParentCategoryId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided categoryId is a sub category, sub category can't be change to parent category");
            }

            Category parentCategory = categoryRepository.findCustomParentCategoryWithSubCategoriesByUserId(
                    request.getParentCategoryId(), user.getUserId()
            ).orElseThrow(() -> new IllegalStateException("Custom parent category not found with provided parent categoryId !!"));

            boolean isParentChange = !category.getParentCategory().getCategoryId().equals(request.getParentCategoryId());

            if(!category.getCategoryName().equalsIgnoreCase(newCategoryName) || isParentChange) {
                if(categoryRepository.existsByCategoryNameAndParentCategoryCategoryIdAndUserUserId(
                        newCategoryName,
                        parentCategory.getCategoryId(),
                        user.getUserId())){
                    throw new IllegalStateException("Another sub category already exist with same name in the parent category !!");
                }

                category.setCategoryName(newCategoryName);
                if(isParentChange){
                    category.setParentCategory(parentCategory);
                }
            }
        }

        category.setDescription(request.getDescription());
        Category savedCategory = categoryRepository.save(category);
        categoryRepository.flush();
        return modelMapper.map(savedCategory, CategoryResponse.class);
    }

    @Override
    @Transactional
    public void deleteCategory(String email, Long categoryId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        Category category = categoryRepository
                .findCustomCategoryOrSubCategoryByUserId(categoryId, user.getUserId()).orElseThrow(() -> new IllegalStateException("Category or SubCategory not found with provided categoryId !!"));

        Long existCategoryId = category.getCategoryId();

        if(category.getParentCategory() == null){
            if(categoryRepository.countBudgetsByCategoryId(existCategoryId)>0){
                throw new IllegalStateException("Can't delete parent category, few budgets are currently using the provided category");
            }
            Set<Category> subCategories = category.getSubCategories();
            for(Category subCategory: subCategories){
                if(categoryRepository.countItemsByCategoryId(subCategory.getCategoryId())>0){
                    throw new IllegalStateException("Can't delete parent category, few items are currently using the subcategory of parent category with id: " + existCategoryId);
                }
            }
            category.getSubCategories().clear();
        }else{
            if(categoryRepository.countItemsByCategoryId(existCategoryId)>0){
                throw new IllegalStateException("Can't delete sub category, few items are currently using the provided category");
            }
        }

        categoryRepository.delete(category);
    }

    @Override
    public PageableResponse<ItemsResponseDto> getAllItemsBySubCategory(String email,
                                                           int budgetId,
                                                           Long subCategoryId,
                                                           int pageNumber,
                                                           int pageSize,
                                                           String sortBy,
                                                           String sortDir) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with provided email."));

        // fetch budget to check ownership and existence
        Budget budget = budgetRepository.findBudgetByUser(budgetId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found"));

        Category subCategory = categoryRepository.findSubCategoryByCategoryIdAndUserId(subCategoryId, user.getUserId())
                .orElseThrow(() -> new IllegalStateException("SubCategory not found with provided subCategoryId !!"));

        if(!budget.getCategory().getCategoryId().equals(subCategory.getParentCategory().getCategoryId())){
            throw new IllegalStateException("sub category does not belongs to the budget's parent category !!");
        }

        Sort sort=(sortDir.equalsIgnoreCase("desc"))?(Sort.by(sortBy).descending()):(Sort.by(sortBy).ascending());
        Pageable pageable = PageRequest.of(pageNumber,pageSize,sort);

        Page<Items> itemsList = itemRepository
                .findByBudgetBudgetIdAndCategoryCategoryId(budget.getBudgetId(), subCategory.getCategoryId(), pageable);

        return helper.getPageableResponse(itemsList, ItemsResponseDto.class);
    }

    @Override
    public CategoryResponses createDefaultCategory(String email, CreateCategoryRequest request) {
        return null;
    }

    @Override
    public CategoryResponses updateDefaultCategory(String email, Long categoryId, UpdateCategoryRequest request) {
        return null;
    }

    @Override
    public void deleteDefaultCategory(String email, Long categoryId) {

    }

    @Override
    public List<CategoryResponses> getCategories(String email) {
        return List.of();
    }
}
