package com.manage_expense.controllers;

import com.manage_expense.config.AppConstants;
import com.manage_expense.dtos.dto_requests.ChangePasswordRequest;
import com.manage_expense.dtos.dto_requests.DeleteUserRequest;
import com.manage_expense.dtos.dto_requests.GoogleAccountPasswordRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.dtos.dto_responses.PageableResponse;
import com.manage_expense.dtos.dto_responses.UserResponseDto;
import com.manage_expense.dtos.dto_responses.UsersResponseDto;
import com.manage_expense.services.services_template.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/user")
@Tag(name = "2. User API", description = "APIs for managing user accounts, including password changes, account deletion, and logout functionality.")
public class UserController {

    @Autowired
    private UserService userService;

    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @GetMapping("/get-all-users")
    @Operation(summary = "7. Get All Users", description = "Retrieve a paginated list of all users. Accessible only by admin users.")
    public ResponseEntity<PageableResponse<UsersResponseDto>> getAllUsers(
            Authentication authentication,
            @RequestParam(value = "pageNumber", defaultValue = "0",required = false) int pageNumber,
            @RequestParam(value = "pageSize",defaultValue = "10",required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "email",required = false) String sortBy,
            @RequestParam(value = "sortDir",defaultValue = "asc",required = false) String sortDir
    ){
        return ResponseEntity.ok(userService.getAllUsers(authentication.getName(),pageNumber,pageSize,sortBy,sortDir));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @GetMapping("/get-user-details")
    @Operation(summary = "1. Get User Details", description = "Retrieve the details of the authenticated user. Accessible by both regular users and admin users.")
    public ResponseEntity<UserResponseDto> getAllUserDetails(Authentication authentication){
        return ResponseEntity.ok(userService.getAllUserDetails(authentication.getName()));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PutMapping("/change-password")
    @Operation(summary = "5. Change Password", description = "Allow authenticated users to change their password. Accessible by both regular users and admin users.")
    public ResponseEntity<ApiResponse> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest changePasswordRequest){
        return ResponseEntity.ok(userService.changePassword(authentication.getName(),changePasswordRequest));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PutMapping("/google-account-set-password")
    @Operation(summary = "4. Set Password for Google Account", description = "Allow users who registered using Google account to set a password for their account. Accessible by both regular users and admin users.")
    public ResponseEntity<ApiResponse> googleAccountPassword(Authentication authentication, @Valid @RequestBody GoogleAccountPasswordRequest request){
        return ResponseEntity.ok(userService.googleAccountPassword(authentication.getName(),request));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @DeleteMapping("/delete-user")
    @Operation(summary = "6. Delete User Account", description = "Allow authenticated users to delete their own account. Accessible by both regular users and admin users.")
    public ResponseEntity<ApiResponse> deleteUser(Authentication authentication, @Valid @RequestBody DeleteUserRequest deleteUserRequest){
        return ResponseEntity.ok(userService.deleteUser(authentication.getName(), deleteUserRequest));
    }

    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @DeleteMapping("/delete-user-by-admin/{userId}")
    @Operation(summary = "8. Delete User Account by Admin", description = "Allow admin users to delete any user account by providing the user ID. Accessible only by admin users.")
    public ResponseEntity<ApiResponse> deleteUserByAdmin(Authentication authentication, @PathVariable int userId){
        return ResponseEntity.ok(userService.deleteUserByAdmin(authentication.getName(), userId));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PostMapping("/logout-all-users")
    @Operation(summary = "3. Logout All Users", description = "Allow authenticated users to log out from all active sessions. Accessible by both regular users and admin users.")
    public ResponseEntity<ApiResponse> logoutAllUsers(Authentication authentication, HttpServletResponse response){
        return ResponseEntity.ok(userService.logoutAllUser(authentication.getName(), response));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PostMapping("/logout-user")
    @Operation(summary = "2. Logout User", description = "Allow authenticated users to log out from the current session. Accessible by both regular users and admin users.")
    public ResponseEntity<ApiResponse> logoutUser(HttpServletRequest request, HttpServletResponse response){
        String sessionId = (String) request.getAttribute("sessionId");
        return ResponseEntity.ok(userService.logoutUser(sessionId, response));
    }
}
