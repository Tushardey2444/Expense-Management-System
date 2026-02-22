package com.manage_expense.dtos.dto_responses;

import com.manage_expense.enums.Gender;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserProfileResponseDto {
    private String firstName;

    private String lastName;

    private String phoneNumber;

    private boolean isPhoneNumberVerified;

    private String profilePictureUrl;

    private String googleProfilePictureUrl;

    private LocalDate dateOfBirth;

    private Gender gender;

    private Date updateAt;
}
