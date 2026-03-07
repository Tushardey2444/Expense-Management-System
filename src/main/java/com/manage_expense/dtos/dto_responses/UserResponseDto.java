package com.manage_expense.dtos.dto_responses;

import lombok.*;

import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserResponseDto {
    private int userId;
    private String email;
    private boolean isPasswordUpdated;
    private Set<RoleResponseDto> roles;
    private UserProfileResponseDto userProfile;
}
