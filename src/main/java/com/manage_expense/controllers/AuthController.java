package com.manage_expense.controllers;

import com.cloudinary.api.exceptions.AlreadyExists;
import com.manage_expense.dtos.dto_requests.*;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.services.services_template.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.password.CompromisedPasswordException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.management.relation.RoleNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
// This is not needed as we have configured CORS in SecurityConfig,
// but if we need to enable CORS for specific endpoints or override global CORS settings,
// then uncomment the @CrossOrigin annotation below and configure it as needed.
//@CrossOrigin(
//        origins = "http://localhost:3000",
//        allowCredentials = "true"
//)
@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Auth API", description = "Operations related to authentication, user management and account recovery")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "4. User Login", operationId = "4", description = "Authenticate user and return JWT token along with setting refresh token in HttpOnly cookie")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody JwtRequest jwtRequest,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws IOException {
        return ResponseEntity.ok(authService.login(jwtRequest, request, response));
    }



    @Operation(summary = "5. Login with Refresh Token",
            description = "Authenticate user using the refresh token stored in HttpOnly cookie and return a new JWT access token, along with setting a new refresh token in the cookie",
            operationId = "5",
            security = {
                @SecurityRequirement(name = "refreshToken")
            }
    ) // for swagger documentation to indicate that this endpoint requires the refresh token cookie for authentication
    @PostMapping("/login-with-refresh-token")
    public ResponseEntity<ApiResponse> loginWithRefreshToken(@Valid @RequestBody RefreshTokenRequest refreshToken,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws IOException{
        return ResponseEntity.ok(authService.loginWithRefreshToken(refreshToken, request, response));
    }

    @PostMapping("/google-auth-url")
    @Operation(summary = "2. Get Google Authentication URL", operationId = "2", description = "Generate and return the Google OAuth 2.0 authentication URL for user login")
    public ResponseEntity<ApiResponse> getGoogleAuthUrl(@Valid @RequestBody GoogleAuthUrlRequest request, HttpServletRequest servletRequest) throws Exception{
        ApiResponse apiResponse = authService.getGoogleAuthUrl(request, servletRequest);
        return new ResponseEntity<>(apiResponse, apiResponse.getStatus());
    }

    @GetMapping("/google/callback")
    @Hidden
    public void googleCallback(@RequestParam(required = false) String code,
                                                        @RequestParam(required = false) String state,
                                                        @RequestParam(required = false, name = "error") String oauthError,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) throws MessagingException, Exception {
        authService.googleCallback(code, state, oauthError, request, response);
    }

    @PostMapping("/register")
    @Operation(summary = "1. User Registration", operationId = "1", description = "Register a new user with email and password, send OTP for verification, and return appropriate response")
    public ResponseEntity<ApiResponse> regiter(@Valid @RequestBody RegisterRequest registerRequest) throws MessagingException, CompromisedPasswordException, RoleNotFoundException, AlreadyExists {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "3. Verify OTP", operationId = "3", description = "Verify the OTP sent to user's email during registration or password recovery and activate the account or allow password reset")
    public ResponseEntity<ApiResponse> verify(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) throws UsernameNotFoundException, AlreadyExists, TimeoutException, MessagingException {
        return new ResponseEntity<>(authService.verify(verifyOtpRequest), HttpStatus.CREATED);
    }

    @PostMapping("/forget-password")
    @Operation(summary = "6. Forget Password", operationId = "6", description = "Initiate the password recovery process by sending an OTP to the user's registered email address for verification")
    public ResponseEntity<ApiResponse> forgetPassword(@Valid @RequestBody ForgetPasswordRequest forgetPasswordRequest) throws MessagingException{
        ApiResponse apiResponse=authService.forgetPassword(forgetPasswordRequest);
        return new ResponseEntity<>(apiResponse,apiResponse.getStatus());
    }

    @PutMapping("/password-update")
    @Operation(summary = "7. Password Update", operationId = "7", description = "Update the user's password after verifying the OTP sent to their email during the forget password process")
    public ResponseEntity<ApiResponse> passwordUpdate(@Valid @RequestBody PasswordUpdateRequest passwordUpdateRequest) throws MessagingException{
        ApiResponse apiResponse=authService.passwordUpdate(passwordUpdateRequest);
        return new ResponseEntity<>(apiResponse,apiResponse.getStatus());
    }

    @PostMapping("/re-activate-deleted-account")
    @Operation(summary = "8. Activate Deleted Account", operationId = "8", description = "Reactivate a previously deleted account by verifying the OTP sent to the user's email address")
    public ResponseEntity<ApiResponse> activeDeletedAccount(@Valid @RequestBody JwtRequest jwtRequest) throws AlreadyExists, MessagingException {
        ApiResponse apiResponse=authService.activeDeletedAccount(jwtRequest);
        return new ResponseEntity<>(apiResponse,apiResponse.getStatus());
    }
}