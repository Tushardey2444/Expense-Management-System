package com.manage_expense.config;

import java.time.Duration;
import java.util.List;

public class AppConstants {
    // ------------------- General Constants ---------------------
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final int MAX_DEVICES_ALLOWED = 3;
    public static final int EXPIRATION = 30;
    public static final int REFRESH_TOKEN_EXPIRATION_DAY = 1;
    public static final int ACCOUNT_DELETION = 30;
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCK_TIME_DURATION_MINUTES = 30;
    public static final int DELETE_BUDGET = 7;
    public static final List<String> PUBLIC_URLS = List.of(
            "/api/auth/",
            "/api/auth/login-with-refresh-token"
    );

    // ------------------- Twilio ---------------------
    public static final String APPROVED = "approved";
    public static final String COUNTRY_CODE = "+91";
    public static final String SMSMedium = "sms";

    // ------------------- Cloudinary ---------------------
    public static final String SECURE_URL= "secure_url";

    // --------------------- Redis ---------------------
    // RedisRateLimiter (Specific API rate limiting)
    public static final String RATE_LIMIT_PREFIX = "rate:";

    // RedisRateLimiter (Successful login attempt rate limiting by IP)
    public static final String RATE_LOGIN_IP_LIMIT_PREFIX = "IP:login:";
    public static final int RATE_LOGIN_IP_LIMIT_MAX_ATTEMPTS = 10;
    public static final Duration RATE_LOGIN_IP_LIMIT_TTL = Duration.ofMinutes(1);

    // RedisRateLimiter (Global rate limiting)
    public static final String GLOBAL_RATE_LIMIT_PREFIX = "rate:global";
    public static final int GLOBAL_RATE_LIMIT_MAX_ATTEMPTS = 2000;
    public static final Duration GLOBAL_RATE_LIMIT_TTL = Duration.ofMinutes(1);

    // RedisRateLimiter (User specific rate limiting)
    public static final String USER_RATE_LIMIT_PREFIX = "rate:user:";
    public static final int USER_RATE_LIMIT_MAX_ATTEMPTS = 120;
    public static final Duration USER_RATE_LIMIT_TTL = Duration.ofMinutes(1);

    // RedisPkceStore (Pkce state and verifier)
    public static final String OAUTH_PREFIX = "oauth:pkce:register:";
    public static final Duration PKCE_TTL = Duration.ofMinutes(5);

    // RedisOtpStore (use email encryption or hashing in production for better security)
    public static final String OTP_PREFIX = "otp:register:";
    public static final Duration OTP_TTL = Duration.ofMinutes(5);
    // RedisOtpStore (Only used for forget password flow to track verified OTPs)
    public static final String OTP_VERIFIED_PREFIX = "otp:verified:";
    public static final Duration OTP_VERIFIED_TTL = Duration.ofMinutes(30);

    // Helper (generateOtp), AuthServiceImpl (verify) (use email encryption or hashing in production for better security)
    public static final String OTP_SAVE = "otp:save:";
    public static final int OTP_MAX_ATTEMPT = 3;
    public static final String OTP_CONSUME = "otp:consume:";
    public static final int OTP_CONSUME_MAX_ATTEMPT = 5;
    public static final Duration OTP_MAX_ATTEMPT_WINDOW = Duration.ofMinutes(5);

    // PkceService (generate), AuthServiceImpl (googleCallback)
    public static final String PKCE_SAVE = "pkce:save:";
    public static final int PKCE_SAVE_MAX_ATTEMPT = 10;
    public static final String PKCE_CONSUME = "pkce:consume:";
    public static final int PKCE_CONSUME_MAX_ATTEMPT = 5;
    public static final Duration PKCE_MAX_ATTEMPT_WINDOW = Duration.ofMinutes(5);

    // --------------------- Cron & Task Scheduler ---------------------
    public static final String TEST_CRON = "*/5 * * * * *";
    // 30 minutes cron
    public static final String HALF_HOUR_SCHEDULAR_CRON = "0 30 * * * *";
    public static final String ONE_HOUR_SCHEDULAR_CRON = "0 0 0/1 * * *";
    // 12:00:01 AM runs on daily basis
    public static final String MIDNIGHT_CRON = "1 0 0 * * *";
    public static final String THREAD_NAME_PREFIX = "Scheduler-";
    public static final int POOL_SIZE = 20;

    // --------------------- Google Config Constants ---------------------
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String GOOGLE_ISSUER = "https://accounts.google.com";
    public static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    public static final String GOOGLE_USERINFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";
    public static final String GOOGLE_GRANT_TYPE = "authorization_code";

    // --------------------- Frontend Application URLs ---------------------
    public static final String APP_LOGIN_URL = "http://localhost:3000/api/auth/login";
}
