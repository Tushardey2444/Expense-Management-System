package com.manage_expense.services.services_template;

import com.manage_expense.dtos.dto_requests.UserProfileRequest;
import com.manage_expense.dtos.dto_requests.VerifyCodeMobileRequest;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public interface UserProfileService {
    ApiResponse updateProfile(String email, UserProfileRequest userProfileRequest);
    ApiResponse updateProfilePicture(String email, MultipartFile file) throws IOException;
    ApiResponse verifyMobileNumber(String email);
    ApiResponse checkVerificationCode(String email, VerifyCodeMobileRequest verifyCodeMobileRequest);
}
