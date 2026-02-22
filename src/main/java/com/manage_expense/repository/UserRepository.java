package com.manage_expense.repository;

import com.manage_expense.entities.User;
import com.manage_expense.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {

    Optional<User> findByEmail(String email);

    @EntityGraph(
            type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {
                    "roles",
                    "userProfile"
            }
    )
    Optional<User> findWithDetailsByEmail(String email);

    // Pagination can't be applied here directly due to EntityGraph limitations.
    @EntityGraph(
            type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {
                    "budgets"
            }
    )
    Optional<User> findBudgetsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findForAuthByEmail(String email);

    void deleteByEmail(String email);

    List<User> findByStatusIn(List<Status> statusList);

    List<User> findByStatus(Status status);

    @EntityGraph(
            type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {"roles", "userProfile"}
    )
    @Query(value = """
        SELECT u
        FROM User u
        JOIN u.roles r
        WHERE r.roleName = :roleName
          AND NOT EXISTS (
              SELECT r2
              FROM User u2
              JOIN u2.roles r2
              WHERE u2 = u
              AND r2.roleName <> :roleName
          )
    """, countQuery = """
        SELECT COUNT(u)
        FROM User u
        JOIN u.roles r
        WHERE r.roleName = :roleName
          AND NOT EXISTS (
              SELECT r2
              FROM User u2
              JOIN u2.roles r2
              WHERE u2 = u
              AND r2.roleName <> :roleName
          )
    """)
    Page<User> findUsersWithOnlySpecificRole(
            @Param("roleName") String roleName,
            Pageable pageable
    );
}
