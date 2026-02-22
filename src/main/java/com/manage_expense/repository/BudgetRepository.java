package com.manage_expense.repository;

import com.manage_expense.entities.Budget;
import com.manage_expense.enums.BudgetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Integer> {
    List<Budget> findByBudgetStatusIn(List<BudgetStatus> budgetStatuses);
    List<Budget> findByBudgetStatus(BudgetStatus budgetStatus);

//    @Query("""
//        select distinct b from Budget b
//        left join fetch b.items
//        where b.budgetId = :budgetId
//        and b.user.userId = :userId
//    """)
//    Optional<Budget> findByIdWithItems(int budgetId, int userId);

    @Query("""
        select b from Budget b
        where b.budgetId = :budgetId
        and b.user.userId = :userId
    """)
    Optional<Budget> findBudgetByUser(int budgetId, int userId);

    @Query(value = """
      select b
      from Budget b
      where b.user.userId = :userId
    """, countQuery = """
      select count(b)
      from Budget b
      where b.user.userId = :userId
    """
    )
    Page<Budget> findBudgetsByUser(
            int userId,
            Pageable pageable
    );

    @Query("""
    SELECT b
    FROM Budget b
    WHERE b.budgetId IN :ids
    AND b.user.userId = :userId
    """)
    List<Budget> findAllByIdsAndUserId(
            @Param("ids") List<Integer> ids,
            @Param("userId") int userId
    );
}
