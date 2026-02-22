package com.manage_expense.services.services_impl;

import com.manage_expense.config.AppConstants;
import com.manage_expense.entities.RefreshToken;
import com.manage_expense.entities.User;
import com.manage_expense.entities.UserSession;
import com.manage_expense.repository.RefreshTokenRepository;
import com.manage_expense.repository.UserRepository;
import com.manage_expense.services.services_template.RefreshTokenService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public RefreshToken generateRefreshToken(User user, UserSession session) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        RefreshToken refreshToken = RefreshToken.builder()
                .refreshToken(token)
                .expiryDate(Instant.now()
                        .plus(Duration.ofDays(AppConstants.REFRESH_TOKEN_EXPIRATION_DAY)))
                .user(user)
                .userSession(session)
                .build();
        user.getRefreshTokens().add(refreshToken);
        session.setRefreshToken(refreshToken);
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken findByToken(String refreshToken) {
        return refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid RefreshToken !!"));
    }

    public boolean verifyRefreshToken(RefreshToken refreshToken) {
        if(refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            return false;
        }
        return true;
    }

    @Scheduled(cron = AppConstants.ONE_HOUR_SCHEDULAR_CRON)
    public void deleteExpiredRefreshTokens(){
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
    }
}
