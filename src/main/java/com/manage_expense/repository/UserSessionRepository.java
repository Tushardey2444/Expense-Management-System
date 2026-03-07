package com.manage_expense.repository;

import com.manage_expense.entities.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    @Query("""
        select s from UserSession s
        where s.user.userId = :userId and s.revoked = false
        order by s.createdAt asc
    """)
    List<UserSession> findActiveSessions(int userId);

    @Modifying
    @Query("""
        UPDATE UserSession s
        SET s.revoked = true
        WHERE s.user.userId = :userId
            AND s.revoked = false
        """)
    void revokeAllByUserId(int userId);

    @Modifying
    @Query("""
        DELETE FROM UserSession s
        WHERE s.revoked = true
            OR s.lastAccessedAt < :cutoff
        """)
    void deleteOldSessions(Instant cutoff);
}
