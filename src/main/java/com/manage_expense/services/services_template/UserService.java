package com.manage_expense.services.services_template;

import com.manage_expense.dtos.dto_requests.ChangePasswordRequest;
import com.manage_expense.dtos.dto_requests.DeleteUserRequest;
import com.manage_expense.dtos.dto_requests.GoogleAccountPasswordRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.dtos.dto_responses.PageableResponse;
import com.manage_expense.dtos.dto_responses.UserResponseDto;
import com.manage_expense.dtos.dto_responses.UsersResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

public interface UserService {
    PageableResponse<UsersResponseDto> getAllUsers(String email, int pageNumber, int pageSize, String sortBy, String sortDir);
    UserResponseDto getAllUserDetails(String email);
    ApiResponse changePassword(String email, ChangePasswordRequest changePasswordRequest);
    ApiResponse googleAccountPassword(String email, GoogleAccountPasswordRequest request);
    ApiResponse deleteUser(String email, DeleteUserRequest deleteUserRequest);
    ApiResponse deleteUserByAdmin(String email, int userId);
    ApiResponse logoutAllUser(String email, HttpServletResponse response);
    ApiResponse logoutUser(String sessionId, HttpServletResponse response);
}
