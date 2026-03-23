package com.manage_expense.controllers;

import com.manage_expense.dtos.dto_requests.UserProfileRequest;
import com.manage_expense.dtos.dto_requests.UserProfileVerifyMobile;
import com.manage_expense.dtos.dto_requests.VerifyCodeMobileRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.services.services_template.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/user-profile")
@Tag(name = "3. User Profile API", description = "Endpoints for managing user profiles")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @PutMapping("/update-profile")
    @Operation(summary = "1. Update User Profile", description = "Update the user's profile information")
    public ResponseEntity<ApiResponse> updateProfile(Authentication authentication, @Valid @RequestBody UserProfileRequest userProfileRequest){
        return ResponseEntity.ok(userProfileService.updateProfile(authentication.getName(), userProfileRequest));
    }

    @PutMapping(value = "/update-profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "2. Update Profile Picture", description = "Update the user's profile picture")
    public ResponseEntity<ApiResponse> updateProfilePicture(Authentication authentication, @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(userProfileService.updateProfilePicture(authentication.getName(), file));
    }

    @PostMapping("/update-mobile-number")
    @Operation(summary = "3. Verify Mobile Number", description = "Send a verification code to the user's provided mobile number for verification")
    public ResponseEntity<ApiResponse> verifyMobileNumber(Authentication authentication,
                                                          @Valid @RequestBody UserProfileVerifyMobile userProfileVerifyMobile){
        return ResponseEntity.ok(userProfileService.verifyMobileNumber(authentication.getName(), userProfileVerifyMobile));
    }

    @PostMapping("/verify-mobile-number")
    @Operation(summary = "4. Check Verification Code", description = "Check the verification code sent to the user's mobile number")
    public ResponseEntity<ApiResponse> checkVerificationCode(Authentication authentication, @Valid @RequestBody VerifyCodeMobileRequest verifyCodeMobileRequest){
        return ResponseEntity.ok(userProfileService.checkVerificationCode(authentication.getName(), verifyCodeMobileRequest));
    }
}