package com.manage_expense.services.services_template;

import com.cloudinary.api.exceptions.AlreadyExists;
import com.manage_expense.dtos.dto_requests.*;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.password.CompromisedPasswordException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.management.relation.RoleNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;


public interface AuthService {
    ApiResponse login(JwtRequest jwtRequest, HttpServletRequest request, HttpServletResponse response) throws IOException;
    ApiResponse loginWithRefreshToken(RefreshTokenRequest refreshTokenRequest, HttpServletRequest request, HttpServletResponse response) throws IOException;
    ApiResponse getGoogleAuthUrl(GoogleAuthUrlRequest request, HttpServletRequest servletRequest) throws Exception;
    void googleCallback(String code, String state, String oauthError, HttpServletRequest request, HttpServletResponse response) throws MessagingException, Exception;
    ApiResponse register(RegisterRequest registerRequest) throws MessagingException, CompromisedPasswordException, RoleNotFoundException, AlreadyExists;
    ApiResponse verify(VerifyOtpRequest verifyOtpRequest) throws UsernameNotFoundException, AlreadyExists, TimeoutException, MessagingException;
    ApiResponse forgetPassword(ForgetPasswordRequest forgetPasswordRequest) throws MessagingException;
    ApiResponse passwordUpdate(PasswordUpdateRequest passwordUpdateRequest) throws MessagingException;
    ApiResponse activeDeletedAccount(JwtRequest jwtRequest) throws AlreadyExists, MessagingException;
}
