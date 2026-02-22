package com.manage_expense.repository;

import com.manage_expense.entities.RefreshToken;
import com.manage_expense.entities.User;
import com.manage_expense.entities.UserSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    @EntityGraph(
            type = EntityGraph.EntityGraphType.LOAD,
            attributePaths = {
                    "user"
            }
    )
    Optional<RefreshToken> findByUser(User user);

    void deleteByExpiryDateBefore(Instant now);

    void deleteByUser(User user);

    void deleteByUserSession(UserSession session);
}
