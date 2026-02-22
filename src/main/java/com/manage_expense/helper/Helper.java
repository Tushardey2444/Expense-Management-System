package com.manage_expense.helper;

import com.manage_expense.config.AppConstants;
import com.manage_expense.dtos.dto_responses.PageableResponse;
import com.manage_expense.redis.RedisRateLimiter;
import org.apache.commons.codec.digest.DigestUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Component
public class Helper {
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RedisRateLimiter redisRateLimiter;

    public <U,V> PageableResponse<V> getPageableResponse(Page<U> page, Class<V> type){
        List<U> entityList=page.getContent();
        List<V> responseDtoList=new ArrayList<>();
        for(U entity :entityList){
            responseDtoList.add(modelMapper.map(entity,type));
        }
        PageableResponse<V> response=new PageableResponse<>();
        response.setContent(responseDtoList);
        response.setPageNumber(page.getNumber()+1);
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLastPage(page.isLast());
        return response;
    }

    public String generateOtp(String email){

        String emailKey = DigestUtils.sha256Hex(email.trim().toLowerCase());
        redisRateLimiter.checkRateLimit(
                AppConstants.OTP_SAVE + emailKey,
                AppConstants.OTP_MAX_ATTEMPT,
                AppConstants.OTP_MAX_ATTEMPT_WINDOW
        );

        SecureRandom secureRandom = new SecureRandom();
        int number = secureRandom.nextInt(1_000_0);
        StringBuilder otp=new StringBuilder();
        otp.append(String.format("%04d", number));
        otp.insert(0, (secureRandom.nextInt(8)+1));
        otp.insert(0, (secureRandom.nextInt(8)+1));
        return otp.toString();
    }

    public static String extractUsernameFromEmail(String email){
        if(email==null || !email.contains("@")){
            return "User";
        }
        return Character.toUpperCase(email.charAt(0))+email.substring(1,email.indexOf("@"));
    }
}
