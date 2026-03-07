package com.manage_expense.services.services_impl;

import com.manage_expense.dtos.dto_requests.*;
import com.manage_expense.dtos.dto_responses.ApiResponse;
import com.manage_expense.entities.User;
import com.manage_expense.entities.UserProfile;
import com.manage_expense.enums.Channel;
import com.manage_expense.exceptions.PublishingException;
import com.manage_expense.rabbitmq.MailMessageProducer;
import com.manage_expense.repository.UserProfileRepository;
import com.manage_expense.repository.UserRepository;
import com.manage_expense.services.services_template.CloudinaryService;
import com.manage_expense.notification.sms.SmsService;
import com.manage_expense.services.services_template.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;

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
        if(userProfileRequest.getPhoneNumber() != null){
            userProfile.setPhoneNumber(userProfileRequest.getPhoneNumber());
            userProfile.setPhoneNumberVerified(false);
            isChange = true;
        }
        if(isChange){
            userProfile.setUpdateAt(new Date());
            userRepository.save(user);
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
    public ApiResponse verifyMobileNumber(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Please enter valid email which is registered with us!!"));

        if(user.isEnabled()){
            UserProfile userProfile = user.getUserProfile();
            String mobileNumber = userProfile.getPhoneNumber();
            if(mobileNumber==null){
                throw new VerifyError("Please update your mobile number in profile section before verification!!");
            }
            try{
                MailMessageDto mailMessageDto = MailMessageDto.builder()
                        .toPhoneNumber(mobileNumber)
                        .body("Your verification code has been sent to your registered mobile number. Please enter the code to verify your mobile number.")
                        .channel(Channel.SMS)
                        .build();

                mailMessageProducer.sendVerificationSms(mailMessageDto);
            }catch (Exception ex) {
                log.error("Failed to publish sms message", ex);
                throw new PublishingException("Could not queue sms", ex);
            }
            return ApiResponse.builder()
                    .message("Verification code send successfully")
                    .success(true)
                    .status(HttpStatus.OK)
                    .build();
        }
        return ApiResponse.builder()
                .message("User not verified with provided email!!")
                .success(false)
                .status(HttpStatus.NOT_FOUND)
                .build();
    }

    @Transactional(noRollbackFor = PublishingException.class)
    @Override
    public ApiResponse checkVerificationCode(String email, VerifyCodeMobileRequest verifyCodeMobileRequest) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Please enter valid email which is registered with us!!"));

        if(user.isEnabled()){
            UserProfile userProfile = user.getUserProfile();
            String mobileNumber = userProfile.getPhoneNumber();
            if(mobileNumber==null){
                throw new VerifyError("Please update your mobile number in profile section to start verification!!");
            }
            SmsCheckVerificationRequest smsCheckVerificationRequest = SmsCheckVerificationRequest.builder()
                    .code(verifyCodeMobileRequest.getCode())
                    .phone(mobileNumber)
                    .build();
            try{
                ApiResponse isVerified = smsService.checkVerification(smsCheckVerificationRequest);
                if(isVerified.isSuccess()){
                    userProfile.setPhoneNumberVerified(true);
                    userProfileRepository.save(userProfile);
                }

                try{
                    MailMessageDto mailMessageDto = MailMessageDto.builder()
                            .toPhoneNumber(mobileNumber)
                            .body("Your mobile number verification status: " + (isVerified.isSuccess() ? "Verified" : "Failed") + ". If you did not attempt this verification, please contact support immediately.")
                            .channel(Channel.SMS)
                            .build();

                    mailMessageProducer.sendVerificationSms(mailMessageDto);
                }catch (Exception ex) {
                    log.error("Failed to publish sms message", ex);
                    throw new PublishingException("Could not queue sms", ex);
                }

                return isVerified;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        return ApiResponse.builder()
                .message("User not verified with provided email!!")
                .success(false)
                .status(HttpStatus.NOT_FOUND)
                .build();
    }
}
