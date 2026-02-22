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
public class UserController {

    @Autowired
    private UserService userService;

    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @GetMapping("/get-all-users")
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
    public ResponseEntity<UserResponseDto> getAllUserDetails(Authentication authentication){
        return ResponseEntity.ok(userService.getAllUserDetails(authentication.getName()));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest changePasswordRequest){
        return ResponseEntity.ok(userService.changePassword(authentication.getName(),changePasswordRequest));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PutMapping("/google-account-set-password")
    public ResponseEntity<ApiResponse> googleAccountPassword(Authentication authentication, @Valid @RequestBody GoogleAccountPasswordRequest request){
        return ResponseEntity.ok(userService.googleAccountPassword(authentication.getName(),request));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @DeleteMapping("/delete-user")
    public ResponseEntity<ApiResponse> deleteUser(Authentication authentication, @Valid @RequestBody DeleteUserRequest deleteUserRequest){
        return ResponseEntity.ok(userService.deleteUser(authentication.getName(), deleteUserRequest));
    }

    @PreAuthorize("hasRole('"+ AppConstants.ROLE_ADMIN+"')")
    @DeleteMapping("/delete-user-by-admin/{userId}")
    public ResponseEntity<ApiResponse> deleteUserByAdmin(Authentication authentication, @PathVariable int userId){
        return ResponseEntity.ok(userService.deleteUserByAdmin(authentication.getName(), userId));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PostMapping("/logout-all-users")
    public ResponseEntity<ApiResponse> logoutAllUsers(Authentication authentication, HttpServletResponse response){
        return ResponseEntity.ok(userService.logoutAllUser(authentication.getName(), response));
    }

    @PreAuthorize("hasAnyRole('"+ AppConstants.ROLE_USER +"', '"+AppConstants.ROLE_ADMIN+"')")
    @PostMapping("/logout-user")
    public ResponseEntity<ApiResponse> logoutUser(HttpServletRequest request, HttpServletResponse response){
        String sessionId = (String) request.getAttribute("sessionId");
        return ResponseEntity.ok(userService.logoutUser(sessionId, response));
    }
}
