package com.manage_expense.services.services_impl;

import com.manage_expense.config.AppConstants;
import com.manage_expense.dtos.dto_requests.*;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.entities.User;
import com.manage_expense.entities.UserProfile;
import com.manage_expense.enums.Channel;
import com.manage_expense.exceptions.PublishingException;
import com.manage_expense.rabbitmq.MailMessageProducer;
import com.manage_expense.redis.RedisRateLimiter;
import com.manage_expense.repository.UserProfileRepository;
import com.manage_expense.repository.UserRepository;
import com.manage_expense.services.services_template.CloudinaryService;
import com.manage_expense.notification.sms.SmsService;
import com.manage_expense.services.services_template.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private MailMessageProducer mailMessageProducer;

    @Autowired
    private SmsService smsService;

    @Autowired
    private RedisRateLimiter rateLimiter;

    @Transactional
    @Override
    public ApiResponse updateProfile(String email, UserProfileRequest userProfileRequest){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Please enter valid email which is registered with us!!"));
        boolean isChange = false;
        UserProfile userProfile = user.getUserProfile();

        if(userProfileRequest.getFirstName() != null){
            userProfile.setFirstName(userProfileRequest.getFirstName());
            isChange = true;
        }
        if(userProfileRequest.getLastName() != null){
            userProfile.setLastName(userProfileRequest.getLastName());
            isChange = true;
        }
        if(userProfileRequest.getDateOfBirth() != null){
            userProfile.setDateOfBirth(userProfileRequest.getDateOfBirth());
            isChange = true;
        }
        if(userProfileRequest.getGender() != null){
            userProfile.setGender(userProfileRequest.getGender());
            isChange = true;
        }

        if(isChange){
            userProfile.setUpdateAt(new Date());
            userProfileRepository.save(userProfile);
            return ApiResponse.builder()
                    .message("Profile updated successfully")
                    .status(HttpStatus.OK)
                    .success(true)
                    .build();
        }else{
            return ApiResponse.builder()
                    .message("Profile update request is empty !!")
                    .status(HttpStatus.BAD_REQUEST)
                    .success(false)
                    .build();
        }
    }

    @Transactional
    @Override
    public ApiResponse updateProfilePicture(String email, MultipartFile file) throws IOException{
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Please enter valid email which is registered with us!!"));
        String url = null;
        if(file==null){
            throw new IOException("Please select a profile picture of size upto 2MB !!");
        }
        if(!file.isEmpty()){
            String originalName = file.getOriginalFilename();
            if(originalName!=null){

                int dot = originalName.lastIndexOf(".");
                String extension = dot == -1 ? "" : originalName.substring(dot + 1);

                if(extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg")){
                    try {
                        url = cloudinaryService.upload(file);
                    }catch (IOException e){
                        throw new IOException("Failed to upload profile picture, please try again!!");
                    }
                }
            }
        }

        ApiResponse apiResponse = ApiResponse.builder().build();

        if(url==null){
            apiResponse.setMessage("Provided file is not valid, please select a valid profile picture of size upto 2MB.");
            apiResponse.setStatus(HttpStatus.BAD_REQUEST);
            apiResponse.setSuccess(false);
            return apiResponse;
        }
        UserProfile userProfile = user.getUserProfile();
        userProfile.setProfilePictureUrl(url);
        userProfile.setUpdateAt(new Date());
        apiResponse.setMessage(url);
        apiResponse.setSuccess(true);
        apiResponse.setStatus(HttpStatus.OK);
        return apiResponse;
    }

    @Override
    @Transactional(noRollbackFor = PublishingException.class)
    public ApiResponse verifyMobileNumber(String email, UserProfileVerifyMobile userProfileVerifyMobile) {

        rateLimiter.checkRateLimit(
                AppConstants.SMS_OTP_RATE_LIMIT_PREFIX + DigestUtils.sha256Hex(email),
                AppConstants.SMS_OTP_MAX_ATTEMPT,
                AppConstants.SMS_OTP_RATE_LIMIT_TTL
        );

        String phoneNumber = userProfileVerifyMobile.getPhoneNumber();

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Please enter valid email which is registered with us!!"));
        UserProfile existingUserProfile = userProfileRepository.findByPhoneNumber(phoneNumber).orElse(null);

        if(existingUserProfile!=null){
             if(existingUserProfile.getUser().getUserId() != user.getUserId()) {
                 if(existingUserProfile.isPhoneNumberVerified()) {
                     return ApiResponse.builder()
                             .message("This mobile number is already verified with another account!!")
                             .success(false)
                             .status(HttpStatus.BAD_REQUEST)
                             .build();
                 }else {
                     userProfileRepository.clearPhoneNumberFromOtherUsers(phoneNumber, user.getUserId());
                 }
             }else {
                 if(existingUserProfile.isPhoneNumberVerified()){
                     return ApiResponse.builder()
                             .message("This mobile number is already verified with your account!!")
                             .success(false)
                             .status(HttpStatus.BAD_REQUEST)
                             .build();
                 }
             }
        }

        UserProfile userProfile = user.getUserProfile();
        userProfile.setPhoneNumber(phoneNumber);
        userProfile.setPhoneNumberVerified(false);
        userProfile.setPhoneNumberUpdatedAt(Instant.now());
        UserProfile savedUserProfile = null;

        try {
            savedUserProfile = userProfileRepository.save(userProfile);
        } catch (DataIntegrityViolationException ex) {
            return ApiResponse.builder()
                    .message("This mobile number is already in use")
                    .success(false)
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        try {
            MailMessageDto mailMessageDto = MailMessageDto.builder()
                    .toPhoneNumber(savedUserProfile.getPhoneNumber())
                    .body("Your verification code has been sent to your mobile number. Please enter the code to verify your mobile number.")
                    .channel(Channel.SMS)
                    .build();

            mailMessageProducer.sendVerificationSms(mailMessageDto);
        } catch (Exception ex) {
            log.error("Failed to publish sms message for phone {}", savedUserProfile.getPhoneNumber(), ex);
            throw new PublishingException("Could not queue sms", ex);
        }
        return ApiResponse.builder()
                .message("Verification code sent successfully")
                .success(true)
                .status(HttpStatus.OK)
                .build();
    }

    @Transactional(noRollbackFor = PublishingException.class)
    @Override
    public ApiResponse checkVerificationCode(String email, VerifyCodeMobileRequest verifyCodeMobileRequest) {

        String code = verifyCodeMobileRequest.getCode();

        rateLimiter.checkRateLimit(
                AppConstants.SMS_OTP_VERIFY_RATE_LIMIT_PREFIX + DigestUtils.sha256Hex(code),
                AppConstants.SMS_OTP_VERIFY_MAX_ATTEMPT,
                AppConstants.SMS_OTP_VERIFY_RATE_LIMIT_TTL
        );

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email + ", please register or login with valid email!!"));

        UserProfile userProfile = user.getUserProfile();
        String mobileNumber = userProfile.getPhoneNumber();

        if (mobileNumber == null) {
            return ApiResponse.builder()
                    .message("No mobile number found for this account. Please enter a mobile number first!!")
                    .success(false)
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        SmsCheckVerificationRequest smsCheckVerificationRequest = SmsCheckVerificationRequest.builder()
                .code(code)
                .phone(mobileNumber)
                .build();
        try {
            ApiResponse isVerified = smsService.checkVerification(smsCheckVerificationRequest);

            if (isVerified.isSuccess()) {
                userProfile.setPhoneNumberVerified(true);
                userProfileRepository.save(userProfile);
            }

            try {
                MailMessageDto mailMessageDto = MailMessageDto.builder()
                        .toPhoneNumber(mobileNumber)
                        .body("Your mobile number verification status: " + (isVerified.isSuccess() ? "Verified" : "Failed") + ". If you did not attempt this verification, please contact support immediately.")
                        .channel(Channel.SMS)
                        .build();

                mailMessageProducer.sendSms(mailMessageDto);

            } catch (Exception ex) {
                log.error("Failed to publish sms message for phone {}", mobileNumber, ex);
                throw new PublishingException("Could not queue sms", ex);
            }

            return isVerified;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Scheduled(cron = AppConstants.HALF_HOUR_SCHEDULAR_CRON)
    @Transactional
    public void cleanUpUnverifiedPhoneNumbers() {

        Instant expiryTime =
                Instant.now().minus(AppConstants.EXPIRATION, ChronoUnit.MINUTES);

        int cleanedPhoneNumbers = userProfileRepository.clearExpiredPhoneNumbers(expiryTime);

        log.info("Cleaned {} expired unverified phone numbers", cleanedPhoneNumbers);
    }
}
