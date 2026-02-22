package com.manage_expense.controllers;

import com.manage_expense.dtos.dto_requests.UserProfileRequest;
import com.manage_expense.dtos.dto_requests.VerifyCodeMobileRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.services.services_template.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/user-profile")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @PutMapping("/update")
    public ResponseEntity<ApiResponse> updateProfile(Authentication authentication, @Valid @RequestBody UserProfileRequest userProfileRequest){
        return ResponseEntity.ok(userProfileService.updateProfile(authentication.getName(), userProfileRequest));
    }

    @PutMapping("/update-profile-picture")
    public ResponseEntity<ApiResponse> updateProfilePicture(Authentication authentication, @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(userProfileService.updateProfilePicture(authentication.getName(), file));
    }

    @PostMapping("/verify-mobile-number")
    public ResponseEntity<ApiResponse> verifyMobileNumber(Authentication authentication){
        return ResponseEntity.ok(userProfileService.verifyMobileNumber(authentication.getName()));
    }

    @PostMapping("/verify-code-mobile-number")
    public ResponseEntity<ApiResponse> checkVerificationCode(Authentication authentication, @Valid @RequestBody VerifyCodeMobileRequest verifyCodeMobileRequest){
        return ResponseEntity.ok(userProfileService.checkVerificationCode(authentication.getName(), verifyCodeMobileRequest));
    }
}
