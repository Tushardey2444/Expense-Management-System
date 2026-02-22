package com.manage_expense.dtos.dto_responses;

import com.manage_expense.enums.Status;
import lombok.*;

import java.util.Date;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UsersResponseDto {
    private int userId;
    private String email;
    private boolean isPasswordUpdated;
    private Date createdAt;
    private boolean isActive;
    private Status status;
    private Set<RoleResponseDto> roles;
    private UserProfileResponseDto userProfile;
}
