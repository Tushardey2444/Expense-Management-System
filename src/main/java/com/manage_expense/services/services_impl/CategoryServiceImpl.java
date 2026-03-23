package com.manage_expense.services.services_impl;

import com.manage_expense.dtos.dto_requests.CreateCategoryRequest;
import com.manage_expense.dtos.dto_requests.UpdateCategoryRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.dtos.dto_responses.CategoryResponse;
import com.manage_expense.dtos.dto_responses.ItemsResponseDto;
import com.manage_expense.dtos.dto_responses.PageableResponse;
import com.manage_expense.entities.*;
import com.manage_expense.helper.Helper;
import com.manage_expense.repository.BudgetRepository;
import com.manage_expense.repository.CategoryRepository;
import com.manage_expense.repository.ItemRepository;
import com.manage_expense.repository.UserRepository;
import com.manage_expense.services.services_template.CategoryService;
import com.manage_expense.services.services_template.CloudinaryService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

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
    private CloudinaryService cloudinaryService;

    @Autowired
    private Helper helper;

    @Value("${category_icon_url}")
    private String categoryIconUrl;

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<CategoryResponse> getParentCategories(String email,
                                                      int pageNumber,
                                                      int pageSize,
                                                      String sortBy,
                                                      String sortDir) {

        User user = userRepository.findForAuthByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        Sort sort=(sortDir.equalsIgnoreCase("desc"))?(Sort.by(sortBy).descending()):(Sort.by(sortBy).ascending());
        Pageable pageable = PageRequest.of(pageNumber,pageSize,sort);

        Page<Category> parentCategories = categoryRepository.findParentCategoriesForUser(user.getUserId(), pageable);

        return helper.getPageableResponse(parentCategories, CategoryResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<CategoryResponse> getSubCategories(String email,
                                              Long categoryId,
                                              int pageNumber,
                                              int pageSize,
                                              String sortBy,
                                              String sortDir) {
        User user = userRepository.findForAuthByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        Category parentCategory = categoryRepository.findDefaultOrCustomParentCategoryByUserId(categoryId, user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Default or custom parent category not found with provided categoryId"));

        Sort sort=(sortDir.equalsIgnoreCase("desc"))?(Sort.by(sortBy).descending()):(Sort.by(sortBy).ascending());

        Pageable pageable = PageRequest.of(pageNumber,pageSize,sort);

        Page<Category> subCategories = categoryRepository.findCustomOrDefaultSubCategoriesForParentCategory(parentCategory.getCategoryId(), user.getUserId(), pageable);

        return helper.getPageableResponse(subCategories, CategoryResponse.class);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(String email, CreateCategoryRequest request) {
        User user = userRepository.findForAuthByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        String categoryName  = request.getCategoryName().trim();
        Category category = Category.builder()
                .categoryName(categoryName)
                .categoryIcon(categoryIconUrl)
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
                    .findCustomOrDefaultParentCategoryWithSubCategoriesByUserId(request.getParentCategoryId(), user.getUserId())
                    .orElseThrow(() -> new IllegalStateException("Default or custom parent category not found with provided parent categoryId !!"));

            if(categoryRepository.searchDefaultOrCustomSubCategoryWithSubCategoryName(categoryName, parentCategory.getCategoryId(), user.getUserId())){
                throw new IllegalStateException("Default or custom sub category already exists with provided name under parent category !!");
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
    public ApiResponse updateCategoryIcon(String email, Long categoryId, MultipartFile file) throws IOException {
        User user = userRepository.findForAuthByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email !!"));

        Category category = categoryRepository.findCustomCategoryOrSubCategory(categoryId, user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Custom category or subCategory not found with provided categoryId !!"));

        String url = null;

        if(file==null){
            throw new IOException("Please select a category icon of size upto 1MB !!");
        }

        if(!file.isEmpty()){
            String originalName = file.getOriginalFilename();
            if(originalName!=null){

                int dot = originalName.lastIndexOf(".");
                String extension = dot == -1 ? "" : originalName.substring(dot + 1);

                if(extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg")){
                    try {
                        url = cloudinaryService.upload(file);
                    }catch (IOException e){
                        throw new IOException("Failed to upload category icon, please try again !!");
                    }
                }else{
                    throw new IllegalArgumentException("File extension supported only JPG, PNG and JPEG !!");
                }
            }
        }

        ApiResponse apiResponse = ApiResponse.builder().build();

        if(url==null){
            apiResponse.setMessage("Provided file is not valid, please select a valid category icon of size upto 1MB.");
            apiResponse.setStatus(HttpStatus.BAD_REQUEST);
            apiResponse.setSuccess(false);
            return apiResponse;
        }

        category.setCategoryIcon(url);
        categoryRepository.save(category);

        apiResponse.setMessage(url);
        apiResponse.setSuccess(true);
        apiResponse.setStatus(HttpStatus.OK);
        return apiResponse;
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(String email, Long categoryId, UpdateCategoryRequest request) {
        User user = userRepository.findForAuthByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email !!"));

        Category category = categoryRepository.findCustomCategoryOrSubCategory(categoryId, user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Custom category or subCategory not found with provided categoryId !!"));

        String newCategoryName = request.getCategoryName().trim();

        Category parentCategory = category.getParentCategory();

        if (parentCategory == null) {

            if (!category.getCategoryName().equalsIgnoreCase(newCategoryName)) {
                Optional<Category> existCategory = categoryRepository.findParentCategoryByNameForUserOrDefault(
                        newCategoryName,
                        user.getUserId()
                );

                if (existCategory.isPresent()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default or custom parent category already exist with same category name");
                }

                category.setCategoryName(newCategoryName);
            }

        } else {

            if(!category.getCategoryName().equalsIgnoreCase(newCategoryName)) {
                if(categoryRepository.searchDefaultOrCustomSubCategoryWithSubCategoryName(
                        newCategoryName,
                        parentCategory.getCategoryId(),
                        user.getUserId())){
                    throw new IllegalStateException("Default or custom sub category already exists with provided name under parent category !!");
                }
                category.setCategoryName(newCategoryName);
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
        User user = userRepository.findForAuthByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        Category category = categoryRepository
                .findCustomCategoryOrSubCategoryByUserId(categoryId, user.getUserId()).orElseThrow(() -> new IllegalStateException("Custom category or subCategory not found with provided categoryId !!"));

        Long existCategoryId = category.getCategoryId();

        if(category.getParentCategory() == null){
            if(categoryRepository.countBudgetsByCategoryId(existCategoryId)>0){
                throw new IllegalStateException("Can't delete parent category, few budgets are currently using the provided category");
            }
            Set<Category> subCategories = category.getSubCategories();
            for(Category subCategory: subCategories){
                if(categoryRepository.countItemsByCategoryId(subCategory.getCategoryId())>0){
                    throw new IllegalStateException("Can't delete parent category, few items are currently using the subcategory which is under the parent category with id: " + existCategoryId);
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
    public CategoryResponse createDefaultCategory(String email, CreateCategoryRequest request) {
        userRepository.findForAuthByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        String categoryName  = request.getCategoryName().trim();
        Category category = Category.builder()
                .categoryName(categoryName)
                .categoryIcon(categoryIconUrl)
                .isDefault(true)
                .description(request.getDescription())
                .parentCategory(null)
                .user(null)
                .build();

        Long parentCategoryId = null;

        if(request.getParentCategoryId() == null){

            if(categoryRepository.existsByCategoryNameAndParentCategoryIsNull(categoryName)){
                throw new IllegalStateException("Default or Custom parent category already exists for an existing user with provided category name");
            }

        }else{
            Category parentCategory = categoryRepository
                    .findDefaultParentCategory(request.getParentCategoryId())
                    .orElseThrow(() -> new IllegalStateException("Default parent category not found with provided parent categoryId !!"));

            if(categoryRepository.existsByCategoryNameAndParentCategoryCategoryId(categoryName, parentCategory.getCategoryId())){
                throw new IllegalStateException("Default subCategory already exists with provided name under default parent category !!");
            }

            parentCategoryId = parentCategory.getCategoryId();
            category.setParentCategory(parentCategory);
            parentCategory.getSubCategories().add(category);
        }

        Category savedCategory = categoryRepository.save(category);
        categoryRepository.flush();
        CategoryResponse categoryResponse = modelMapper.map(savedCategory, CategoryResponse.class);
        categoryResponse.setParentCategoryId(parentCategoryId);

        return categoryResponse;
    }

    @Override
    public CategoryResponse updateDefaultCategory(String email, Long categoryId, UpdateCategoryRequest request) {
        userRepository.findForAuthByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email !!"));

        Category category = categoryRepository.findPartialDefaultCategoryOrSubCategory(categoryId)
                .orElseThrow(() -> new IllegalStateException("Default category or subCategory not found with provided categoryId !!"));

        String newCategoryName = request.getCategoryName().trim();

        Category parentCategory = category.getParentCategory();

        if (parentCategory == null) {

            if (!category.getCategoryName().equalsIgnoreCase(newCategoryName)) {
                if(categoryRepository.existsByCategoryNameAndParentCategoryIsNull(newCategoryName)){
                    throw new IllegalStateException("Default or custom parent category already exists for some existing users with provided category name");
                }

                category.setCategoryName(newCategoryName);
            }

        } else {

            if(!category.getCategoryName().equalsIgnoreCase(newCategoryName)) {
                if(categoryRepository.searchDefaultSubCategoryWithSubCategoryName(
                        newCategoryName,
                        parentCategory.getCategoryId())){
                    throw new IllegalStateException("Default or custom subCategory already exists for some users with provided name under the parent category !!");
                }
                category.setCategoryName(newCategoryName);
            }
        }

        category.setDescription(request.getDescription());
        Category savedCategory = categoryRepository.save(category);
        categoryRepository.flush();
        return modelMapper.map(savedCategory, CategoryResponse.class);
    }

    @Override
    public ApiResponse updateDefaultCategoryIcon(String email, Long categoryId, MultipartFile file) throws IOException{
        userRepository.findForAuthByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email !!"));

        Category category = categoryRepository.findPartialDefaultCategoryOrSubCategory(categoryId)
                .orElseThrow(() -> new IllegalStateException("Default category or subCategory not found with provided categoryId !!"));

        String url = null;

        if(file==null){
            throw new IOException("Please select a category icon of size upto 1MB !!");
        }

        if(!file.isEmpty()){
            String originalName = file.getOriginalFilename();
            if(originalName!=null){

                int dot = originalName.lastIndexOf(".");
                String extension = dot == -1 ? "" : originalName.substring(dot + 1);

                if(extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg")){
                    try {
                        url = cloudinaryService.upload(file);
                    }catch (IOException e){
                        throw new IOException("Failed to upload category icon, please try again !!");
                    }
                }else{
                    throw new IllegalArgumentException("File extension supported only JPG, PNG and JPEG !!");
                }
            }
        }

        ApiResponse apiResponse = ApiResponse.builder().build();

        if(url==null){
            apiResponse.setMessage("Provided file is not valid, please select a valid category icon of size upto 1MB.");
            apiResponse.setStatus(HttpStatus.BAD_REQUEST);
            apiResponse.setSuccess(false);
            return apiResponse;
        }

        category.setCategoryIcon(url);
        categoryRepository.save(category);

        apiResponse.setMessage(url);
        apiResponse.setSuccess(true);
        apiResponse.setStatus(HttpStatus.OK);
        return apiResponse;
    }

    @Override
    public void deleteDefaultCategory(String email, Long categoryId) {
        userRepository.findForAuthByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email !!"));

        Category category = categoryRepository
                .findDefaultCategoryOrSubCategory(categoryId).orElseThrow(() -> new IllegalStateException("Default Category or SubCategory not found with provided categoryId !!"));

        Long existCategoryId = category.getCategoryId();

        if(category.getParentCategory() == null){
            if(categoryRepository.countBudgetsByCategoryId(existCategoryId)>0){
                throw new IllegalStateException("Can't delete default parent category as this category contains budgets for some users !!");
            }
            Set<Category> subCategories = category.getSubCategories();
            for(Category subCategory: subCategories){
               if(!subCategory.isDefault()){
                   throw new IllegalStateException("Can't delete default parent category as this category contains custom subCategories for some users !!");
               }
            }
            category.getSubCategories().clear();
        }else{
            if(categoryRepository.countItemsByCategoryId(existCategoryId)>0){
                throw new IllegalStateException("Can't delete default subCategory, few user's items are currently using the subCategory !!");
            }
        }

        categoryRepository.delete(category);
    }

    @Override
    public PageableResponse<String> getSuggestionsOfParentCategoryNameForUser(String email,
                                                                        int pageNumber,
                                                                        int pageSize,
                                                                        String sortBy,
                                                                        String sortDir) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with provided email."));

        Sort sort=(sortDir.equalsIgnoreCase("desc"))?(Sort.by(sortBy).descending()):(Sort.by(sortBy).ascending());
        Pageable pageable = PageRequest.of(pageNumber,pageSize,sort);

        Page<String> subCategoryNameList = categoryRepository
                .findCustomParentCategoryNameSuggestions(user.getUserId(), pageable);

        return helper.getPageableResponse(subCategoryNameList, String.class);
    }

    @Override
    public PageableResponse<String> getSuggestionsOfSubCategoryNameForUser(String email,
                                                                           int pageNumber,
                                                                           int pageSize,
                                                                           String sortBy,
                                                                           String sortDir) {
        User user = userRepository.findForAuthByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with provided email."));

        Sort sort=(sortDir.equalsIgnoreCase("desc"))?(Sort.by(sortBy).descending()):(Sort.by(sortBy).ascending());
        Pageable pageable = PageRequest.of(pageNumber,pageSize,sort);

        Page<String> subCategoryNameList = categoryRepository
                .findCustomSubCategoryNameSuggestions(user.getUserId(), pageable);

        return helper.getPageableResponse(subCategoryNameList, String.class);
    }
}
