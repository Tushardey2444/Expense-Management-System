package com.manage_expense.repository;

import com.manage_expense.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // To check if a parent category with the same name already exists for a user (including default categories)
    boolean existsByCategoryNameAndUser_UserIdAndParentCategoryIsNull(String categoryName, int userId);

    // To check if a default parent category with the same name already exists
    boolean existsByCategoryNameAndUserIsNullAndParentCategoryIsNull(String categoryName);

    // To check if a sub category with the same name already exists for a user (including default categories)
    @Query("""
        SELECT COUNT(c) > 0 FROM Category c
        WHERE LOWER(c.categoryName) = LOWER(:categoryName)
        AND c.parentCategory.categoryId = :parentId
        AND (c.user.userId = :userId OR c.isDefault = true)
    """)
    boolean searchDefaultOrCustomSubCategoryWithSubCategoryName(String categoryName, Long parentId, int userId);

    @Query("""
        SELECT COUNT(c) > 0 FROM Category c
        WHERE LOWER(c.categoryName) = LOWER(:categoryName)
        AND c.parentCategory.categoryId = :parentId
    """)
    boolean searchDefaultSubCategoryWithSubCategoryName(String categoryName, Long parentId);

    boolean existsByCategoryNameAndParentCategoryIsNull(String categoryName);

    boolean existsByCategoryNameAndParentCategoryCategoryId(String categoryName, Long categoryId);

    @Query(value = """
        SELECT c
        FROM Category c
        WHERE c.parentCategory.categoryId = :categoryId
        AND (c.user.userId = :userId OR c.isDefault = true)
    """, countQuery = """
        SELECT count(*)
        FROM Category c
        WHERE c.parentCategory.categoryId = :categoryId
        AND (c.user.userId = :userId OR c.isDefault = true)
    """)
    Page<Category> findCustomOrDefaultSubCategoriesForParentCategory(
                                                        @Param("categoryId") Long categoryId,
                                                        @Param("userId") int userId,
                                                        Pageable pageable);


    @Query("""
        SELECT c
        FROM Category c
        WHERE c.categoryId = :categoryId
        AND c.isDefault = true
        AND c.parentCategory IS NULL
        AND c.user IS NULL
    """)
    Optional<Category> findDefaultParentCategory(@Param("categoryId") Long categoryId);

    @Query(value = """
        SELECT c
        FROM Category c
        WHERE c.parentCategory IS NULL
        AND (c.isDefault = true OR c.user.userId = :userId)
    """, countQuery = """
        SELECT count(*)
        FROM Category c
        WHERE c.parentCategory IS NULL
        AND (c.isDefault = true OR c.user.userId = :userId)
    """)
    Page<Category> findParentCategoriesForUser(@Param("userId") int userId, Pageable pageable);

    @Query("""
        SELECT DISTINCT c
        FROM Category c
        LEFT JOIN FETCH c.subCategories
        WHERE c.categoryId = :categoryId
        AND (c.user.userId = :userId OR c.isDefault = true)
        AND c.parentCategory IS NULL
    """)
    Optional<Category> findCustomOrDefaultParentCategoryWithSubCategoriesByUserId(@Param("categoryId") Long categoryId,
                                                          @Param("userId") int userId);

    @Query("""
        SELECT DISTINCT c
        FROM Category c
        LEFT JOIN FETCH c.subCategories
        WHERE c.categoryId = :categoryId
        AND c.isDefault = true
        AND c.user IS NULL
    """)
    Optional<Category> findDefaultCategoryOrSubCategory(@Param("categoryId") Long categoryId);

    @Query("""
        SELECT DISTINCT c
        FROM Category c
        WHERE c.categoryId = :categoryId
        AND c.isDefault = true
        AND c.user IS NULL
    """)
    Optional<Category> findPartialDefaultCategoryOrSubCategory(@Param("categoryId") Long categoryId);

    @Query("""
        SELECT DISTINCT c
        FROM Category c
        LEFT JOIN FETCH c.subCategories
        WHERE c.categoryId = :categoryId
        AND c.user.userId = :userId
        AND c.isDefault = false
    """)
    Optional<Category> findCustomCategoryOrSubCategoryByUserId(@Param("categoryId") Long categoryId,
                                                                         @Param("userId") int userId);

    @Query("""
        SELECT DISTINCT c
        FROM Category c
        WHERE c.categoryId = :categoryId
        AND c.user.userId = :userId
        AND c.isDefault = false
    """)
    Optional<Category> findCustomCategoryOrSubCategory(@Param("categoryId") Long categoryId,
                                                               @Param("userId") int userId);

    @Query("""
        SELECT DISTINCT c
        FROM Category c
        WHERE c.categoryId = :categoryId
        AND (c.user.userId = :userId OR c.isDefault = true)
        AND c.parentCategory IS NULL
    """)
    Optional<Category> findDefaultOrCustomParentCategoryByUserId(@Param("categoryId") Long categoryId,
                                                                       @Param("userId") int userId);


    @Query("""
        SELECT COUNT(b)
        FROM Budget b
        WHERE b.category.categoryId = :categoryId
    """)
    long countBudgetsByCategoryId(Long categoryId);

    @Query("""
        SELECT COUNT(i)
        FROM Items i
        WHERE i.category.categoryId = :categoryId
    """)
    long countItemsByCategoryId(Long categoryId);


    @Query("""
        SELECT c
        FROM Category c
        WHERE c.categoryId = :subCategoryId
        AND c.parentCategory IS NOT NULL
        AND (c.user.userId = :userId OR c.isDefault = true)
    """)
    Optional<Category> findSubCategoryByCategoryIdAndUserId(@Param("subCategoryId") Long subCategoryId,
                                                            @Param("userId") int userId);

//    @Query(value = """
//      select i
//      from Items i
//      where i.budget.budgetId = :budgetId
//      AND i.category.categoryId = :subCategoryId
//    """, countQuery = """
//      select count(i)
//      from Items i
//      where i.budget.budgetId = :budgetId
//      AND i.category.categoryId = :subCategoryId
//    """
//    )
//    Page<Items> findItemsBySubCategoryId(
//            @Param("subCategoryId") Long subCategoryId,
//            @Param("budgetId") int budgetId,
//            Pageable pageable
//    );

    @Query("""
        SELECT c
        FROM Category c
        WHERE LOWER(c.categoryName) = LOWER(:categoryName)
        AND c.parentCategory IS NULL
        AND (c.user.userId = :userId OR c.isDefault = true)
    """)
    Optional<Category> findParentCategoryByNameForUserOrDefault(
            @Param("categoryName") String categoryName,
            @Param("userId") int userId
    );


    @Query(value = """
        Select c.categoryName
        FROM Category c
        WHERE c.parentCategory IS NULL
        AND c.isDefault = false
        AND c.user.userId != :userId
    """, countQuery = """
        Select count(*)
        FROM Category c
        WHERE c.parentCategory IS NULL
        AND c.isDefault = false
        AND c.user.userId != :userId
    """)
    Page<String> findCustomParentCategoryNameSuggestions(@Param("userId") int userId, Pageable pageable);

    @Query(value = """
        Select c.categoryName
        FROM Category c
        WHERE c.parentCategory IS NOT NULL
        AND c.isDefault = false
        AND c.user.userId != :userId
    """, countQuery = """
        Select count(*)
        FROM Category c
        WHERE c.parentCategory IS NOT NULL
        AND c.isDefault = false
        AND c.user.userId != :userId
    """)
    Page<String> findCustomSubCategoryNameSuggestions(@Param("userId") int userId, Pageable pageable);
}