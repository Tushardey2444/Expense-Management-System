package com.manage_expense.services.services_impl;

import com.manage_expense.config.AppConstants;
import com.manage_expense.redis.RedisRateLimiter;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PkceService {

    @Autowired
    private RedisRateLimiter rateLimiter;

    public record Pkce(String verifier, String challenge) {}

    public Pkce generate(String ip) throws Exception {

        rateLimiter.checkRateLimit(
                AppConstants.PKCE_SAVE + DigestUtils.sha256Hex(ip),
                AppConstants.PKCE_SAVE_MAX_ATTEMPT,
                AppConstants.PKCE_MAX_ATTEMPT_WINDOW
        );

        // RFC 7636: 32 bytes → 43+ chars after Base64 URL encoding
        byte[] verifierBytes = new byte[32];
        SecureRandom.getInstanceStrong().nextBytes(verifierBytes);

        String verifier = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(verifierBytes);

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hash = messageDigest.digest(verifier.getBytes(StandardCharsets.US_ASCII));

        String challenge = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hash);

        return new Pkce(verifier, challenge);
    }
}

