package com.manage_expense.controllers;

import com.cloudinary.api.exceptions.AlreadyExists;
import com.manage_expense.dtos.dto_requests.*;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.services.services_template.AuthService;
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
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody JwtRequest jwtRequest,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws IOException {
        return ResponseEntity.ok(authService.login(jwtRequest, request, response));
    }

    @PostMapping("/login-with-refresh-token")
    public ResponseEntity<ApiResponse> loginWithRefreshToken(@Valid @RequestBody RefreshTokenRequest refreshToken,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws IOException{
        return ResponseEntity.ok(authService.loginWithRefreshToken(refreshToken, request, response));
    }

    @PostMapping("/google-auth-url")
    public ResponseEntity<ApiResponse> getGoogleAuthUrl(@Valid @RequestBody GoogleAuthUrlRequest request, HttpServletRequest servletRequest) throws Exception{
        ApiResponse apiResponse = authService.getGoogleAuthUrl(request, servletRequest);
        return new ResponseEntity<>(apiResponse, apiResponse.getStatus());
    }

    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam(required = false) String code,
                                                        @RequestParam(required = false) String state,
                                                        @RequestParam(required = false, name = "error") String oauthError,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) throws MessagingException, Exception {
        authService.googleCallback(code, state, oauthError, request, response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> regiter(@Valid @RequestBody RegisterRequest registerRequest) throws MessagingException, CompromisedPasswordException, RoleNotFoundException, AlreadyExists {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verify(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) throws UsernameNotFoundException, AlreadyExists, TimeoutException, MessagingException {
        return new ResponseEntity<>(authService.verify(verifyOtpRequest), HttpStatus.CREATED);
    }

    @PostMapping("/forget-password")
    public ResponseEntity<ApiResponse> forgetPassword(@Valid @RequestBody ForgetPasswordRequest forgetPasswordRequest) throws MessagingException{
        ApiResponse apiResponse=authService.forgetPassword(forgetPasswordRequest);
        return new ResponseEntity<>(apiResponse,apiResponse.getStatus());
    }

    @PutMapping("/password-update")
    public ResponseEntity<ApiResponse> passwordUpdate(@Valid @RequestBody PasswordUpdateRequest passwordUpdateRequest) throws MessagingException{
        ApiResponse apiResponse=authService.passwordUpdate(passwordUpdateRequest);
        return new ResponseEntity<>(apiResponse,apiResponse.getStatus());
    }

    @PostMapping("/active-deleted-account")
    public ResponseEntity<ApiResponse> activeDeletedAccount(@Valid @RequestBody JwtRequest jwtRequest) throws AlreadyExists, MessagingException {
        ApiResponse apiResponse=authService.activeDeletedAccount(jwtRequest);
        return new ResponseEntity<>(apiResponse,apiResponse.getStatus());
    }
}
