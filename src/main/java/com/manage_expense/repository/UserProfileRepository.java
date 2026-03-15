package com.manage_expense.repository;

import com.manage_expense.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {

    Optional<UserProfile> findByPhoneNumber(String phoneNumber);

    Optional<UserProfile> findByPhoneNumberAndIsPhoneNumberVerifiedTrue(String phoneNumber);

    Optional<UserProfile> findByIsPhoneNumberVerifiedFalseAndPhoneNumberNotNull();

    @Modifying
    @Query("""
        UPDATE UserProfile u
        SET u.phoneNumber = null,
            u.isPhoneNumberVerified = false,
            u.phoneNumberUpdatedAt = null
        WHERE u.isPhoneNumberVerified = false
        AND u.phoneNumber IS NOT NULL
        AND u.phoneNumberUpdatedAt < :expiryTime
    """)
    int clearExpiredPhoneNumbers(@Param("expiryTime") Instant expiryTime);

    @Modifying
    @Query("""
        UPDATE UserProfile u
        SET u.phoneNumber = null,
            u.isPhoneNumberVerified = false,
            u.phoneNumberUpdatedAt = null
        WHERE u.phoneNumber = :phone
        AND u.isPhoneNumberVerified = false
        AND u.user.userId <> :userId
    """)
    void clearPhoneNumberFromOtherUsers(String phone, int userId);
}
