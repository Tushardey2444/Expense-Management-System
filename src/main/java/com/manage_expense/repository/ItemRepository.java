package com.manage_expense.repository;

import com.manage_expense.entities.Items;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Items,Integer> {

    @Query(value = """
      select i
      from Items i
      where i.budget.budgetId = :budgetId
    """, countQuery = """
      select count(i)
      from Items i
      where i.budget.budgetId = :budgetId
    """
    )
    Page<Items> findItemsByBudget(
            int budgetId,
            Pageable pageable
    );

    @Query("""
        select i from Items i
        where i.itemId = :itemId
        and i.budget.budgetId = :budgetId
    """)
    Optional<Items> findItemByBudget(int itemId, int budgetId);

    @Query("""
    SELECT i
    FROM Items i
    WHERE i.itemId IN :ids
    AND i.budget.budgetId = :budgetId
    """)
    List<Items> findAllByIdsAndBudgetId(
            @Param("ids") List<Integer> ids,
            @Param("budgetId") int budgetId
    );
}
