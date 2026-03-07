package com.manage_expense.services.services_impl;

import com.manage_expense.config.AppConstants;
import com.manage_expense.dtos.dto_requests.*;
import com.manage_expense.dtos.dto_responses.*;
import com.manage_expense.entities.*;
import com.manage_expense.enums.Channel;
import com.manage_expense.enums.Providers;
import com.manage_expense.enums.Status;
import com.manage_expense.exceptions.PublishingException;
import com.manage_expense.helper.Helper;
import com.manage_expense.notification.email.EmailService;
import com.manage_expense.notification.template.AccountDeletionScheduledTemplate;
import com.manage_expense.notification.template.AccountPasswordUpdatedTemplate;
import com.manage_expense.notification.template.GoogleAccountPasswordSetTemplate;
import com.manage_expense.rabbitmq.MailMessageProducer;
import com.manage_expense.repository.RefreshTokenRepository;
import com.manage_expense.repository.UserRepository;
import com.manage_expense.repository.UserSessionRepository;
import com.manage_expense.services.services_template.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Set;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Helper helper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MailMessageProducer mailMessageProducer;

    @Transactional(readOnly = true)
    @Override
    public PageableResponse<UsersResponseDto> getAllUsers(String email, int pageNumber, int pageSize, String sortBy, String sortDir) {
        userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email."));
        Sort sort=(sortDir.equalsIgnoreCase("desc"))?(Sort.by(sortBy).descending()):(Sort.by(sortBy).ascending());
        Pageable pageable= PageRequest.of(pageNumber,pageSize,sort);
        Page<User> page=userRepository.findUsersWithOnlySpecificRole("ROLE_"+ AppConstants.ROLE_USER, pageable);
        return helper.getPageableResponse(page, UsersResponseDto.class);
    }

    @Transactional(readOnly = true)
    @Override
    public UserResponseDto getAllUserDetails(String email) {
        User user = userRepository.findWithDetailsByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email."));
        return modelMapper.map(user, UserResponseDto.class);
    }

    @Transactional(noRollbackFor = PublishingException.class)
    @Override
    public ApiResponse changePassword(String email, ChangePasswordRequest changePasswordRequest) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));
        if (!changePasswordRequest.getPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new BadCredentialsException("New password and Confirm password didn't match!!");
        }
        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password !!");
        }
        refreshTokenRepository.deleteByUser(user);
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getPassword()));
        user.setPasswordUpdated(true);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        // send email to user about password change
        String html = AccountPasswordUpdatedTemplate.build(email);

        try {
            mailMessageProducer.sendEmail(MailMessageDto.builder()
                    .email(email)
                    .subject("Expense Management System || Password Changed Successfully")
                    .body(html)
                    .channel(Channel.EMAIL)
                    .build());

        }catch (Exception ex) {
            log.error("Failed to publish email message", ex);
            throw new PublishingException("Could not queue email", ex);
        }

        UserProfile userProfile = user.getUserProfile();
        String mobileNumber = userProfile.getPhoneNumber();
        if (mobileNumber != null && userProfile.isPhoneNumberVerified()) {
            try {
                MailMessageDto mailMessageDto = MailMessageDto.builder()
                        .toPhoneNumber(mobileNumber)
                        .body("Your account password was successfully changed.")
                        .channel(Channel.SMS)
                        .build();
                mailMessageProducer.sendSms(mailMessageDto);
            }catch (Exception ex) {
                log.error("Failed to publish sms message", ex);
                throw new PublishingException("Could not queue sms", ex);
            }
        }
        return ApiResponse.builder()
                .message("Password changes successfully.")
                .status(HttpStatus.OK)
                .success(true)
                .build();
    }

    @Transactional(noRollbackFor = PublishingException.class)
    @Override
    public ApiResponse googleAccountPassword(String name, GoogleAccountPasswordRequest request) {
        User user = userRepository.findByEmail(name).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));
        if (!user.getProvider().equals(Providers.GOOGLE)) {
            throw new BadCredentialsException("This account is not registered via Google Sign-In.");
        }
        if(user.isPasswordUpdated()){
            throw new BadCredentialsException("Password is already set for this Google account.");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadCredentialsException("New password and Confirm password didn't match!!");
        }
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordUpdated(true);
        userRepository.save(user);

        String email = user.getEmail();

        String html = GoogleAccountPasswordSetTemplate.build(email);

        try {
            mailMessageProducer.sendEmail(MailMessageDto.builder()
                    .email(email)
                    .subject("Expense Management System || Password Set Successfully")
                    .body(html)
                    .channel(Channel.EMAIL)
                    .build());

        }catch (Exception ex) {
            log.error("Failed to publish email message", ex);
            throw new PublishingException("Could not queue email", ex);
        }

        UserProfile userProfile = user.getUserProfile();
        String mobileNumber = userProfile.getPhoneNumber();

        if(mobileNumber!=null && userProfile.isPhoneNumberVerified()){
            try {
                MailMessageDto mailMessageDto = MailMessageDto.builder()
                        .toPhoneNumber(mobileNumber)
                        .body("Your password for Expense Management System has been successfully set. " +
                                "\uD83D\uDD10 You can now sign in using your email and password in addition to Google sign-in." +
                                "If you made this change, no further action is required." +
                                " If you did not make this change, Please reset your password immediately.")
                        .channel(Channel.SMS)
                        .build();
                mailMessageProducer.sendSms(mailMessageDto);
            }catch (Exception ex) {
                log.error("Failed to publish sms message", ex);
                throw new PublishingException("Could not queue sms", ex);
            }
        }

        return ApiResponse.builder()
                .message("Password set successfully. You can now log in using your email and password.")
                .status(HttpStatus.OK)
                .success(true)
                .build();
    }

    @Transactional(noRollbackFor = PublishingException.class)
    @Override
    public ApiResponse deleteUser(String email, DeleteUserRequest deleteUserRequest) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));
        if (!deleteUserRequest.getPassword().equals(deleteUserRequest.getConfirmPassword())) {
            throw new BadCredentialsException("New password and Confirm password didn't match!!");
        }
        refreshTokenRepository.deleteByUser(user);
        user.setStatus(Status.DELETED);
        user.setActive(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        UserProfile userProfile = user.getUserProfile();
        userProfile.setUpdateAt(new Date());
        userRepository.save(user);

        String html = AccountDeletionScheduledTemplate.build(email, AppConstants.APP_LOGIN_URL);

        try {
            mailMessageProducer.sendEmail(MailMessageDto.builder()
                    .email(email)
                    .subject("Expense Management System || Account Deletion Scheduled")
                    .body(html)
                    .channel(Channel.EMAIL)
                    .build());

        }catch (Exception ex) {
            log.error("Failed to publish email message", ex);
            throw new PublishingException("Could not queue email", ex);
        }

        String mobileNumber = userProfile.getPhoneNumber();
        if (mobileNumber != null && userProfile.isPhoneNumberVerified()) {
            try {
                MailMessageDto mailMessageDto = MailMessageDto.builder()
                        .toPhoneNumber(mobileNumber)
                        .body("Your account has been scheduled for deletion within 30 days. If you want to re-active your account then please login.")
                        .channel(Channel.SMS)
                        .build();
                mailMessageProducer.sendSms(mailMessageDto);
            }catch (Exception ex) {
                log.error("Failed to publish sms message", ex);
                throw new PublishingException("Could not queue sms", ex);
            }
        }
        return ApiResponse.builder()
                .message("User's account has been scheduled for deletion within 30 days.")
                .status(HttpStatus.OK)
                .success(true)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse deleteUserByAdmin(String email, int userId) {
        userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        User user = userRepository.findById(userId).orElse(null);
        ApiResponse apiResponse= ApiResponse.builder().build();
        if(user == null){
            apiResponse.setMessage("Provided UserId does not exist !!");
        }else{
            Set<Role> roles = user.getRoles();
            boolean flag=false;
            for(Role role: roles){
                if(role.getRoleName().equals(AppConstants.ROLE_ADMIN)){
                    flag=true;
                    break;
                }
            }
            if(flag){
                apiResponse.setMessage("Provided user is an admin, admin user can't be deleted.");
            }else{
                user.getRoles().clear();
                user.getRefreshTokens().clear();
                userRepository.deleteById(userId);
                apiResponse.setMessage("User Delete Successfully.");
            }
        }
        apiResponse.setSuccess(true);
        apiResponse.setStatus(HttpStatus.OK);
        return apiResponse;
    }

    @Transactional
    @Override
    public ApiResponse logoutAllUser(String email, HttpServletResponse response) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with provided email"));

        // Clear ACCESS token cookie
        ResponseCookie accessCookie = ResponseCookie.from(AppConstants.ACCESS_TOKEN, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // Clear REFRESH token cookie
        ResponseCookie refreshCookie = ResponseCookie.from(AppConstants.REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        userRepository.flush();

        refreshTokenRepository.deleteByUser(user);

        return ApiResponse.builder()
                .message("All sessions logout successfully")
                .status(HttpStatus.OK)
                .success(true)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse logoutUser(String sessionId, HttpServletResponse response) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Clear ACCESS token cookie
        ResponseCookie accessCookie = ResponseCookie.from(AppConstants.ACCESS_TOKEN, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // Clear REFRESH token cookie
        ResponseCookie refreshCookie = ResponseCookie.from(AppConstants.REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Delete refresh token linked to session
        refreshTokenRepository.deleteByUserSession(session);

        return ApiResponse.builder()
                .message("User logout successfully")
                .status(HttpStatus.OK)
                .success(true)
                .build();
    }
    /*
    ResponseCookie:
    When you set a cookie in the browser, it is associated with a specific path.
    So if you don’t clear it using same path, browser will NOT delete it.
    Path must match exactly.
     */
}