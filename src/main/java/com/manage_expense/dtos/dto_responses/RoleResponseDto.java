package com.manage_expense.dtos.dto_responses;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RoleResponseDto {
    private int roleId;
    private String roleName;
}
