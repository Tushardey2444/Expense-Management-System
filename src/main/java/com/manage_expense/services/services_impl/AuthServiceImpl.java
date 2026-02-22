package com.manage_expense.services.services_impl;

import com.cloudinary.api.exceptions.AlreadyExists;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.manage_expense.config.AppConstants;
import com.manage_expense.dtos.dto_requests.*;
import com.manage_expense.dtos.dto_responses.*;
import com.manage_expense.entities.*;
import com.manage_expense.enums.Channel;
import com.manage_expense.enums.Providers;
import com.manage_expense.enums.Status;
import com.manage_expense.exceptions.InvalidOtpException;
import com.manage_expense.exceptions.PublishingException;
import com.manage_expense.exceptions.RateLimitExceededException;
import com.manage_expense.helper.Helper;
import com.manage_expense.helper.OAuth.OAuthState;
import com.manage_expense.rabbitmq.MailMessageProducer;
import com.manage_expense.redis.RedisOtpStore;
import com.manage_expense.redis.RedisPkceStore;
import com.manage_expense.notification.template.*;
import com.manage_expense.redis.RedisRateLimiter;
import com.manage_expense.repository.RefreshTokenRepository;
import com.manage_expense.repository.RoleRepository;
import com.manage_expense.repository.UserRepository;
import com.manage_expense.repository.UserSessionRepository;
import com.manage_expense.security.GoogleTokenVerifier;
import com.manage_expense.security.JwtCookieUtil;
import com.manage_expense.security.JwtHelper;
import com.manage_expense.services.services_template.AuthService;
import com.manage_expense.services.services_template.RefreshTokenService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.password.CompromisedPasswordException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.relation.RoleNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${blank_profile_url}")
    private String profilePictureUrl;

    @Autowired
    private MailMessageProducer mailMessageProducer;

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private GoogleTokenVerifier googleTokenVerifier;

    @Autowired
    private OAuthStateCryptoService crypto;

    @Autowired
    private PkceService pkceService;

    @Autowired
    private RedisPkceStore pkceStore;

    @Autowired
    private RedisOtpStore otpStore;

    @Autowired
    private Helper helper;

    @Autowired
    private RedisRateLimiter rateLimiter;


    @Value("${jwt_expiration}")
    private long expiration;

    @Transactional(noRollbackFor = {BadCredentialsException.class, RateLimitExceededException.class})
    @Override
    public ApiResponse login(JwtRequest jwtRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = userRepository.findByEmail(jwtRequest.getEmail()).orElseThrow(() -> new UsernameNotFoundException("Email not registered with us, please register!!"));

        if(user.isEnabled() && user.getProvider() == Providers.GOOGLE && !user.isPasswordUpdated()){
            throw new BadCredentialsException("Please login with Google Sign-In and set a password from profile settings to use normal login.");
        }

        Instant now = Instant.now();

        if (!user.isEnabled() && user.getStatus() == Status.DELETED) {
            if (passwordEncoder.matches(jwtRequest.getPassword(), user.getPassword())) {
                UserProfile userProfile = user.getUserProfile();
                Instant deleteAt = userProfile.getUpdateAt().toInstant().plus(AppConstants.ACCOUNT_DELETION, ChronoUnit.DAYS);
                if (deleteAt.isAfter(now)) {
                    Duration accountDeletionDuration = Duration.between(now, deleteAt);
                    long remainingDays = accountDeletionDuration.toDays();
                    int remainingHours = accountDeletionDuration.toHoursPart();
                    return ApiResponse.builder()
                            .message("Your account will be deleted in next "
                                    + remainingDays + " day(s) " + remainingHours +" hour(s). Please login to re-activate your account!!")
                            .status(HttpStatus.NOT_ACCEPTABLE)
                            .success(false)
                            .build();
                } else {
                    user.getRoles().clear();
                    user.getRefreshTokens().clear();
                    userRepository.deleteById(user.getUserId());
                    return ApiResponse.builder()
                            .message("Your account has been deleted, please create a new account!!")
                            .status(HttpStatus.NOT_FOUND)
                            .success(false)
                            .build();
                }
            }
        }
        if (!user.isEnabled() && (user.getStatus() == Status.PENDING || user.getStatus() == Status.EXPIRED)) {
            return ApiResponse.builder()
                    .message("Your email is not registered with us, please register your email!!")
                    .status(HttpStatus.NOT_FOUND)
                    .success(false)
                    .build();
        }
        if (user.isAccountLocked() && user.getLockTime() != null) {
            Instant unlockTime = user.getLockTime().toInstant().plus(AppConstants.LOCK_TIME_DURATION_MINUTES, ChronoUnit.MINUTES);
            if (unlockTime.isBefore(now)) {
                resetAttempts(user);
                userRepository.save(user);
            }else{
                long remainingMinutes = Duration.between(now, unlockTime).toMinutes();
                return ApiResponse.builder()
                        .message("Your account has been locked, please try after "+remainingMinutes+" minutes")
                        .status(HttpStatus.NOT_ACCEPTABLE)
                        .success(false)
                        .build();
            }
        }
        try {
            this.doAuthentication(jwtRequest.getEmail(), jwtRequest.getPassword());
        } catch (BadCredentialsException e) {
            loginFailed(user);
            throw new BadCredentialsException(e.getMessage());
        }

        String ipAddress = getClientIp(request);

        rateLimiter.checkRateLimit(
                AppConstants.RATE_LOGIN_IP_LIMIT_PREFIX + ipAddress,
                AppConstants.RATE_LOGIN_IP_LIMIT_MAX_ATTEMPTS,
                AppConstants.RATE_LOGIN_IP_LIMIT_TTL
        );

        if (user.getStatus() == Status.FORGET_PASSWORD) {
            user.setStatus(Status.ACTIVE);
            otpStore.remove(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            otpStore.clearVerified(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            userRepository.save(user);
        }
        if(user.getFailedAttempts()>0){
            user.setFailedAttempts(0);
            userRepository.save(user);
        }

        // Fetch active sessions
        List<UserSession> sessions =
                sessionRepository.findActiveSessions(user.getUserId());

        // Enforce max devices
        if (sessions.size() >= AppConstants.MAX_DEVICES_ALLOWED) {
            UserSession oldest = sessions.getFirst();
            oldest.setRevoked(true);
            oldest.setRefreshToken(null);
            sessionRepository.save(oldest);
        }

        // Create new session
        UserSession session = new UserSession();
        session.setUser(user);
        session.setDeviceId(jwtRequest.getDeviceId());
        session.setDeviceName(jwtRequest.getDeviceName());
        session.setIpAddress(ipAddress);
        session.setCreatedAt(now);
        session.setLastAccessedAt(now);

        UserSession savedSession = sessionRepository.save(session);

        String token = jwtHelper.generateToken(user, savedSession);

        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user, savedSession);

        Duration duration = Duration.ofMillis(expiration);

        /* For Development Cookie Settings */
        ResponseCookie cookie = ResponseCookie.from(AppConstants.ACCESS_TOKEN, token)
                .httpOnly(true) // JS cannot read
                .secure(false)      // Localhost only (true for HTTPS)
                .sameSite("Lax")  // prevents CSRF
                .path("/")      // available to all APIs
                .maxAge(duration)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        /*Production Cookie Settings
        ResponseCookie cookie = ResponseCookie.from(AppConstants.ACCESS_TOKEN, token)
                .httpOnly(true) // JS cannot read
                .secure(true)      // true for HTTPS
                .sameSite("None")  // prevents CSRF
                .path("/")      // available to all APIs
                .maxAge(duration)
                .build();
         */
        ResponseCookie refreshCookie = ResponseCookie.from(AppConstants.REFRESH_TOKEN, refreshToken.getRefreshToken())
                .httpOnly(true)
                .secure(false) // true in production
                .sameSite("Lax") // None in cross-domain production
         //       .path("/api/auth/login-with-refresh-token") // tell browser to send this cookie only to refresh endpoint
                .path("/")
                .maxAge(Duration.ofDays(AppConstants.REFRESH_TOKEN_EXPIRATION_DAY))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

//        /* Redirect to React */
//        response.sendRedirect("http://localhost:3000/dashboard");

        return ApiResponse.builder()
                .message("Login successful!!")
                .status(HttpStatus.OK)
                .success(true)
                .build();
    }

    @Override
    public ApiResponse loginWithRefreshToken(RefreshTokenRequest refreshTokenRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String refreshTokenValue = JwtCookieUtil.extractToken(request, AppConstants.REFRESH_TOKEN);
        if(refreshTokenValue == null){
            return ApiResponse.builder()
                    .message("Refresh token is missing, please login again using email and password!!")
                    .success(false)
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }
        RefreshToken savedRefreshToken = refreshTokenService.findByToken(refreshTokenValue);
        if (!refreshTokenService.verifyRefreshToken(savedRefreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token, please login again!!");
        }
        User user = savedRefreshToken.getUser();
        Instant now = Instant.now();

        if (!user.isEnabled() && user.getStatus() == Status.DELETED) {
            UserProfile userProfile = user.getUserProfile();
            Instant deleteAt = userProfile.getUpdateAt().toInstant().plus(AppConstants.ACCOUNT_DELETION, ChronoUnit.DAYS);
            if (deleteAt.isAfter(now)) {
                Duration accountDeletionDuration = Duration.between(now, deleteAt);
                long remainingDays = accountDeletionDuration.toDays();
                int remainingHours = accountDeletionDuration.toHoursPart();
                return ApiResponse.builder()
                        .message("Your account will be deleted in next "
                                + remainingDays + " day(s) " + remainingHours +" hour(s). Please login to re-activate your account!!")
                        .status(HttpStatus.NOT_ACCEPTABLE)
                        .success(false)
                        .build();
            } else {
                user.getRoles().clear();
                user.getRefreshTokens().clear();
                userRepository.deleteById(user.getUserId());
                return ApiResponse.builder()
                        .message("Your account has been deleted, please create a new account!!")
                        .status(HttpStatus.NOT_FOUND)
                        .success(false)
                        .build();
            }
        }

        if (user.isAccountLocked() && user.getLockTime() != null) {
            Instant unlockTime = user.getLockTime().toInstant().plus(AppConstants.LOCK_TIME_DURATION_MINUTES, ChronoUnit.MINUTES);
            if (unlockTime.isBefore(now)) {
                resetAttempts(user);
                userRepository.save(user);
            }else{
                long remainingMinutes = Duration.between(now, unlockTime).toMinutes();
                return ApiResponse.builder()
                        .message("Your account has been locked, please try after "+remainingMinutes+" minutes")
                        .status(HttpStatus.NOT_ACCEPTABLE)
                        .success(false)
                        .build();
            }
        }

        String ipAddress = getClientIp(request);

        rateLimiter.checkRateLimit(
                AppConstants.RATE_LOGIN_IP_LIMIT_PREFIX + ipAddress,
                AppConstants.RATE_LOGIN_IP_LIMIT_MAX_ATTEMPTS,
                AppConstants.RATE_LOGIN_IP_LIMIT_TTL
        );

        if (user.getStatus() == Status.FORGET_PASSWORD) {
            user.setStatus(Status.ACTIVE);
            otpStore.remove(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            otpStore.clearVerified(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            userRepository.save(user);
        }

        refreshTokenRepository.delete(savedRefreshToken);

        // Fetch active sessions
        List<UserSession> sessions =
                sessionRepository.findActiveSessions(user.getUserId());

        // Enforce max devices
        if (sessions.size() >= AppConstants.MAX_DEVICES_ALLOWED) {
            UserSession oldest = sessions.getFirst();
            oldest.setRevoked(true);
            oldest.setRefreshToken(null);
            sessionRepository.save(oldest);
        }

        // Create new session
        UserSession session = new UserSession();
        session.setUser(user);
        session.setDeviceId(refreshTokenRequest.getDeviceId());
        session.setDeviceName(refreshTokenRequest.getDeviceName());
        session.setIpAddress(getClientIp(request));
        session.setCreatedAt(now);
        session.setLastAccessedAt(now);

        UserSession savedSession = sessionRepository.save(session);

        String token = jwtHelper.generateToken(user, savedSession);

        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user, savedSession);

        Duration duration = Duration.ofMillis(expiration);

        /* For Development Cookie Settings */
        ResponseCookie cookie = ResponseCookie.from(AppConstants.ACCESS_TOKEN, token)
                .httpOnly(true) // JS cannot read
                .secure(false)      // Localhost only (true for HTTPS)
                .sameSite("Lax")  // prevents CSRF
                .path("/")      // available to all APIs
                .maxAge(duration)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        /*Production Cookie Settings
        ResponseCookie cookie = ResponseCookie.from(AppConstants.ACCESS_TOKEN, token)
                .httpOnly(true) // JS cannot read
                .secure(true)      // true for HTTPS
                .sameSite("None")  // prevents CSRF
                .path("/")      // available to all APIs
                .maxAge(duration)
                .build();
         */
        ResponseCookie refreshCookie = ResponseCookie.from(AppConstants.REFRESH_TOKEN, refreshToken.getRefreshToken())
                .httpOnly(true)
                .secure(false) // true in production
                .sameSite("Lax") // None in cross-domain production
                //       .path("/api/auth/login-with-refresh-token") // tell browser to send this cookie only to refresh endpoint
                .path("/")
                .maxAge(Duration.ofDays(AppConstants.REFRESH_TOKEN_EXPIRATION_DAY))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

//        /* Redirect to React */
//        response.sendRedirect("http://localhost:3000/dashboard");

        return ApiResponse.builder()
                .message("Login successful!!")
                .status(HttpStatus.OK)
                .success(true)
                .build();
    }

    @Override
    public ApiResponse getGoogleAuthUrl(GoogleAuthUrlRequest request, HttpServletRequest servletRequest) throws Exception {
        OAuthState state = OAuthState.builder()
                .deviceId(request.getDeviceId())
                .deviceName(request.getDeviceName())
                .timeStamp(Instant.now().getEpochSecond())
                .build();

        String ipAddress = getClientIp(servletRequest);

        String encryptedState = crypto.encrypt(state);
        PkceService.Pkce pkce = pkceService.generate(ipAddress);
        // store pkce.verifier() temporarily in redis
        pkceStore.save(encryptedState, pkce.verifier());

        String authUrl = googleOAuthService.buildGoogleUrl(encryptedState, pkce.challenge());

        return ApiResponse.builder()
                .message(authUrl)
                .status(HttpStatus.OK)
                .success(true)
                .build();
    }

    @Transactional(noRollbackFor = PublishingException.class)
    @Override
    public void googleCallback(String code, String state, String oauthError, HttpServletRequest request, HttpServletResponse response) throws MessagingException, Exception {
        // Handle OAuth errors (e.g., user denied consent)
        if (oauthError != null) {
            response.sendRedirect("http://localhost:3000/login?error=oauth_error");
            return;
        }

        // Validate required parameters
        if (code == null || state == null) {
            response.sendRedirect("http://localhost:3000/login?error=missing_params");
            return;
        }

        /* Decrypt & validate state */
        OAuthState oauthState = crypto.decrypt(state);

        long nowInSeconds = Instant.now().getEpochSecond();
        if ((nowInSeconds - oauthState.getTimeStamp()) > 300) {
            throw new IllegalStateException("OAuth state expired.");
        }

        String ipAddress = getClientIp(request);

        rateLimiter.checkRateLimit(
                AppConstants.PKCE_CONSUME + DigestUtils.sha256Hex(ipAddress),
                AppConstants.PKCE_CONSUME_MAX_ATTEMPT,
                AppConstants.PKCE_MAX_ATTEMPT_WINDOW
        );

        /* Load PKCE verifier */
        /* PKCE -> Proof Key for Code Exchange */
        String codeVerifier = pkceStore.consume(state);
        if (codeVerifier == null) {
            throw new IllegalStateException("Verifier not found or expired.");
        }

        /* Exchange code for tokens and validate ID token */
        GoogleTokenResponse tokenResponse = googleOAuthService.exchangeCode(code, codeVerifier);
        GoogleIdToken.Payload userPayload = googleTokenVerifier.verify(tokenResponse.getIdToken());
//      GoogleUserInfoResponse userInfo = googleOAuthService.getUserInfo(tokenResponse.getAccessToken());

        /* Extract user info from token payload */
        String email = userPayload.getEmail();
        String googleUserId = userPayload.getSubject();
//        String name = (String) userPayload.get("name");
        String pictureUrl = (String) userPayload.get("picture");
//        String locale = (String) userPayload.get("locale");
        String familyName = (String) userPayload.get("family_name");
        String givenName = (String) userPayload.get("given_name");
//        String gender = (String) userPayload.get("gender");
//        String dateOfBirth = (String) userPayload.get("birthdate");
//        String audience = (String) userPayload.getAudience();
//        String issuer = userPayload.getIssuer();
//        long expirationSeconds = userPayload.getExpirationTimeSeconds();
        Boolean emailVerified = userPayload.getEmailVerified();

        if(email == null || email.isEmpty()){
            throw new SecurityException("Email not found in Google ID token");
        }

        if (emailVerified == null || !emailVerified) {
            throw new SecurityException("Email not verified by Google");
        }

        /*  Create / Update user */
        User user = userRepository.findByEmail(email).orElse(null);
        Date now = new Date();

        if (user == null || user.getStatus() == Status.DELETED || user.getStatus() == Status.EXPIRED || user.getStatus() == Status.PENDING) {

            if (user != null && user.getStatus() == Status.DELETED) {
                UserProfile userProfile = user.getUserProfile();
                Instant deleteAt = userProfile.getUpdateAt().toInstant().plus(30, ChronoUnit.DAYS);
                if (deleteAt.isAfter(Instant.now())) {
                    long remainingDays = Duration.between(Instant.now(), deleteAt).toDays();
                    response.sendRedirect("http://localhost:3000/login?error=Account_Deleted&remainingDays=" + remainingDays);
                    return;
                } else {
                    user.getRoles().clear();
                    user.getRefreshTokens().clear();
                    userRepository.deleteById(user.getUserId());
                    user = null;
                }
            }

            if(user == null) {
                Role roleUser = roleRepository.findByRoleName("ROLE_" + AppConstants.ROLE_USER).orElseThrow(
                        () -> new RuntimeException("Failed to fetch role!!")
                );
                user = User.builder()
                        .email(email)
                        .tokenVersion(0)
                        .roles(Set.of(roleUser))
                        .refreshTokens(new HashSet<>())
                        .build();
                roleUser.getUsers().add(user);
            }

            user.setCreatedAt(now);
            user.setPassword(null);
            user.setPasswordUpdated(false);
            user.setProvider(Providers.GOOGLE);
            user.setProviderUserId(googleUserId);
            user.setActive(true);

            UserProfile userProfile = UserProfile.builder()
                    .firstName(givenName)
                    .lastName(familyName)
                    .isPhoneNumberVerified(false)
                    .googleProfilePictureUrl(pictureUrl)
                    .profilePictureUrl(profilePictureUrl)
                    .updateAt(now)
                    .user(user)
                    .build();

            user.setUserProfile(userProfile);

            // sending welcome email
            String html = WelcomeTemplate.build(email, AppConstants.APP_LOGIN_URL);

            try {
                mailMessageProducer.sendEmail(MailMessageDto.builder()
                        .email(email)
                        .subject("Expense Management System || Account Created")
                        .body(html)
                        .channel(Channel.EMAIL)
                        .build());

            }catch (Exception ex) {
                    log.error("Failed to publish email message", ex);
                    throw new PublishingException("Could not queue email", ex);
            }

        } else {
            if(!user.getProvider().equals(Providers.GOOGLE)) {
                UserProfile userProfile = user.getUserProfile();
                user.setProvider(Providers.GOOGLE);
                user.setProviderUserId(googleUserId);
                userProfile.setGoogleProfilePictureUrl(pictureUrl);
                if(userProfile.getFirstName() == null) {
                    userProfile.setFirstName(givenName);
                }
                if(userProfile.getLastName() == null) {
                    userProfile.setLastName(familyName);
                }
                userProfile.setUpdateAt(now);
            }
            resetAttempts(user);
        }

        if(user.getStatus() != Status.ACTIVE){
            otpStore.remove(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            otpStore.clearVerified(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            user.setStatus(Status.ACTIVE);
            userRepository.save(user);
        }

        // Fetch active sessions
        List<UserSession> sessions =
                sessionRepository.findActiveSessions(user.getUserId());

        // Enforce max 3 devices
        if (sessions.size() >= 3) {
            UserSession oldest = sessions.getFirst();
            oldest.setRevoked(true);
            oldest.setRefreshToken(null);
            sessionRepository.save(oldest);
        }

        // Create new session
        UserSession session = new UserSession();
        session.setUser(user);
        session.setDeviceId(oauthState.getDeviceId());
        session.setDeviceName(oauthState.getDeviceName());
        session.setIpAddress(ipAddress);
        session.setCreatedAt(Instant.now());
        session.setLastAccessedAt(Instant.now());

        UserSession savedSession = sessionRepository.save(session);

        /* Generate JWT token */
        String token = jwtHelper.generateToken(user, savedSession);

        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user, savedSession);

        Duration duration = Duration.ofMillis(expiration);

        /* For Development Cookie Settings */
        ResponseCookie cookie = ResponseCookie.from(AppConstants.ACCESS_TOKEN, token)
                .httpOnly(true) // JS cannot read
                .secure(false)      // false for localhost only (true for HTTPS)
                // Lax prevents CSRF for localhost only (None for cross-site in production with HTTPS)
                .sameSite("Lax")
                .path("/")      // available to all APIs
                .maxAge(duration)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        ResponseCookie refreshCookie = ResponseCookie.from(AppConstants.REFRESH_TOKEN, refreshToken.getRefreshToken())
                .httpOnly(true)
                .secure(false) // true in production
                .sameSite("Lax") // None in cross-domain production
                //       .path("/api/auth/login-with-refresh-token") // tell browser to send this cookie only to refresh endpoint
                .path("/")
                .maxAge(Duration.ofDays(AppConstants.REFRESH_TOKEN_EXPIRATION_DAY))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

         /* Redirect to React */
        response.sendRedirect("http://localhost:3000/dashboard");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // First IP is the real client, then proxies e.g. real client, proxy1, proxy2
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public void doAuthentication(String email, String password){
        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email,password));
        }catch(BadCredentialsException ex){
            throw new BadCredentialsException("Invalid email or password!!");
        }
    }

    public void loginFailed(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= AppConstants.MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockTime(new Date());
        }
        userRepository.save(user);
    }

    public void resetAttempts(User user){
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
    }



    @Override
    @Transactional
    public ApiResponse register(RegisterRequest registerRequest) throws MessagingException, CompromisedPasswordException, RoleNotFoundException, AlreadyExists {
        User user = userRepository.findByEmail(registerRequest.getEmailId()).orElse(null);
        if(user != null && user.isEnabled()){
            throw new AlreadyExists("User already exists !!");
        }

        if(user != null && user.getStatus() ==  Status.DELETED){
            UserProfile userProfile = user.getUserProfile();
            Instant deleteAt = userProfile.getUpdateAt().toInstant().plus(30, ChronoUnit.DAYS);
            if(deleteAt.isAfter(Instant.now())) {
                long remainingDays = Duration.between(Instant.now(), deleteAt).toDays();
                throw new IllegalArgumentException("Please login re-activate your account, Your account will be deleted in "
                        + remainingDays + " days.");
            }else{
                userRepository.deleteByEmail(user.getEmail());
                user=null;
            }
        }

        if(!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())){
            throw new CompromisedPasswordException("Password and confirm Password didn't match!!");
        }

        if(user == null){
            Role roleUser = roleRepository.findByRoleName("ROLE_"+AppConstants.ROLE_USER).orElseThrow(
                    () -> new RoleNotFoundException("Failed to fetch role!!")
            );
            user = User.builder()
                    .email(registerRequest.getEmailId())
                    .roles(Set.of(roleUser))
                    .refreshTokens(new HashSet<>())
                    .build();
            roleUser.getUsers().add(user);
        }

        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPasswordUpdated(true);
        user.setStatus(Status.PENDING);
        user.setProvider(Providers.LOCAL);
        user.setTokenVersion(0);
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        user.setActive(false);
        user.setCreatedAt(new Date());

        // Generate OTP
        String generateOtp = helper.generateOtp(user.getEmail());

        // sending otp via mail
        String html = OtpRequestTemplate.build(registerRequest.getEmailId(), generateOtp);

        try {
            mailMessageProducer.sendEmail(MailMessageDto.builder()
                    .email(registerRequest.getEmailId())
                    .subject("Expense Management System || Account OTP Verification")
                    .body(html)
                    .channel(Channel.EMAIL)
                    .build());

        }catch (Exception ex) {
            log.error("Failed to publish email message", ex);
            throw new PublishingException("Could not queue email", ex);
        }

        otpStore.saveOtp(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()), passwordEncoder.encode(generateOtp));

        userRepository.save(user);
        // return response
        return ApiResponse.builder()
                .message("OTP send successfully")
                .status(HttpStatus.OK)
                .success(true)
                .build();
    }

    @Transactional(noRollbackFor = PublishingException.class)
    @Override
    public ApiResponse verify(VerifyOtpRequest verifyOtpRequest) throws UsernameNotFoundException, AlreadyExists, TimeoutException, MessagingException{
        User user = userRepository.findByEmail(verifyOtpRequest.getEmail()).orElse(null);
        if(user == null || user.getStatus() == Status.EXPIRED){
            throw new UsernameNotFoundException("Please register your credentials!!");
        }

        if(user.isEnabled() && user.getStatus()==Status.ACTIVE){
            throw new AlreadyExists("User already exists, please login !!");
        }

        rateLimiter.checkRateLimit(
                AppConstants.OTP_CONSUME + DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()),
                AppConstants.OTP_CONSUME_MAX_ATTEMPT,
                AppConstants.OTP_MAX_ATTEMPT_WINDOW
        );

        String storedOtpHash = otpStore.getOtp(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));

        if(storedOtpHash==null && user.getStatus() == Status.DELETED){
            throw new IllegalArgumentException("User has requested to delete his/her account, please login to re-active your account!!");
        }

        if(storedOtpHash==null){
            throw new IllegalArgumentException("Entered expired or already used OTP, please generate your OTP!!");
        }

        if(!passwordEncoder.matches(verifyOtpRequest.getOTP(), storedOtpHash)){
            throw new InvalidOtpException("Entered Invalid OTP");
        }

        if(!user.isEnabled() && user.getStatus()== Status.DELETED){
            user.setActive(true);
            user.setStatus(Status.ACTIVE);
            UserProfile userProfile = user.getUserProfile();
            userProfile.setUpdateAt(new Date());
            userRepository.save(user);
            otpStore.remove(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            // sending otp via mail
            String html = AccountReactivatedTemplate.build(verifyOtpRequest.getEmail(), AppConstants.APP_LOGIN_URL);

            try {
                mailMessageProducer.sendEmail(MailMessageDto.builder()
                        .email(verifyOtpRequest.getEmail())
                        .subject("Expense Management System || Account Re-Activated")
                        .body(html)
                        .channel(Channel.EMAIL)
                        .build());

            }catch (Exception ex) {
                log.error("Failed to publish email message", ex);
                throw new PublishingException("Could not queue email", ex);
            }

            return ApiResponse.builder()
                    .message("Account has been re-activated successfully")
                    .success(true)
                    .status(HttpStatus.ACCEPTED)
                    .build();

        }else if(!user.isEnabled() && user.getStatus()== Status.PENDING){
            UserProfile userProfile=UserProfile.builder()
                    .profilePictureUrl(profilePictureUrl)
                    .isPhoneNumberVerified(false)
                    .updateAt(new Date())
                    .build();
            userProfile.setUser(user);
            user.setUserProfile(userProfile);
            user.setActive(true);
            user.setStatus(Status.ACTIVE);
            userRepository.save(user);
            otpStore.remove(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            // sending otp via mail
            String html = OtpVerifiedTemplate.build(verifyOtpRequest.getEmail(), AppConstants.APP_LOGIN_URL);

            try {
                mailMessageProducer.sendEmail(MailMessageDto.builder()
                        .email(verifyOtpRequest.getEmail())
                        .subject("Expense Management System || Account Created")
                        .body(html)
                        .channel(Channel.EMAIL)
                        .build());

            }catch (Exception ex) {
                log.error("Failed to publish email message", ex);
                throw new PublishingException("Could not queue email", ex);
            }

            return ApiResponse.builder()
                    .message("User created successfully")
                    .success(true)
                    .status(HttpStatus.CREATED)
                    .build();
        }else{
            otpStore.remove(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            otpStore.markVerified(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            return ApiResponse.builder()
                    .message("OTP verified, please reset your password")
                    .success(true)
                    .status(HttpStatus.OK)
                    .build();
        }
    }

    @Transactional
    @Override
    public ApiResponse forgetPassword(ForgetPasswordRequest forgetPasswordRequest) throws MessagingException {
        User user = userRepository.findByEmail(forgetPasswordRequest.getEmail()).orElse(null);

        if(user!=null && user.isEnabled()){
            // Generate OTP
            String generateOtp = helper.generateOtp(user.getEmail());

            // sending otp via mail
            String html = ForgotPasswordOtpTemplate.build(forgetPasswordRequest.getEmail(), generateOtp);

            try {
                mailMessageProducer.sendEmail(MailMessageDto.builder()
                        .email(forgetPasswordRequest.getEmail())
                        .subject("Expense Management System || Forget Password OTP Verification")
                        .body(html)
                        .channel(Channel.EMAIL)
                        .build());

            }catch (Exception ex) {
                log.error("Failed to publish email message", ex);
                throw new PublishingException("Could not queue email", ex);
            }

            otpStore.saveOtp(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()), passwordEncoder.encode(generateOtp));
            user.setStatus(Status.FORGET_PASSWORD);

            userRepository.save(user);
            return ApiResponse.builder()
                    .message("OTP sent successfully via email")
                    .success(true)
                    .status(HttpStatus.OK)
                    .build();
        }else{
            if(user!=null && user.getStatus()==Status.DELETED){
                throw new IllegalArgumentException("User has requested to delete his/her account, please login to re-active your account!!");
            }
            throw new UsernameNotFoundException("Provided email is not registered, please register!!");
        }
    }

    @Transactional(noRollbackFor = PublishingException.class)
    @Override
    public ApiResponse passwordUpdate(PasswordUpdateRequest passwordUpdateRequest) throws MessagingException {
        User user = userRepository.findByEmail(passwordUpdateRequest.getEmail()).orElse(null);

        if(user == null || (user.getStatus() == Status.EXPIRED || user.getStatus() == Status.PENDING)){
            throw new UsernameNotFoundException("Please register your credentials!!");
        }

        if(user.getStatus() == Status.DELETED){
            throw new IllegalArgumentException("User has requested to delete his/her account, please login to re-active your account!!");
        }

        if(user.getStatus() ==  Status.ACTIVE){
            throw new IllegalStateException("User has not requested for password reset.");
        }

        boolean isVerified = otpStore.isVerified(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));

        if(isVerified) {
            otpStore.clearVerified(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()));
            if (passwordUpdateRequest.getNewPassword().equals(passwordUpdateRequest.getConfirmPassword())) {
                user.setStatus(Status.ACTIVE);
                user.setPassword(passwordEncoder.encode(passwordUpdateRequest.getNewPassword()));
                user.setPasswordUpdated(true);
                user.setTokenVersion(user.getTokenVersion() + 1);
                resetAttempts(user);
                UserProfile userProfile = user.getUserProfile();
                userProfile.setUpdateAt(new Date());
                userRepository.save(user);
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

                String html = AccountPasswordUpdatedTemplate.build(passwordUpdateRequest.getEmail());

                try {
                    mailMessageProducer.sendEmail(MailMessageDto.builder()
                            .email(passwordUpdateRequest.getEmail())
                            .subject("Expense Management System || Password Update")
                            .body(html)
                            .channel(Channel.EMAIL)
                            .build());

                }catch (Exception ex) {
                    log.error("Failed to publish email message", ex);
                    throw new PublishingException("Could not queue email", ex);
                }

                return ApiResponse.builder()
                        .message("User's password updated successfully")
                        .status(HttpStatus.OK)
                        .success(true)
                        .build();
            }else{
                throw new CompromisedPasswordException("Password and confirm Password didn't match!!");
            }
        }else{
            throw new VerifyError("User did not change their password within 30 minutes of OTP verification / User did not requested to update their password / Verification required with OTP to reset your password, please generate your OTP and re-verify to update your password.");
        }
    }

    @Transactional
    @Override
    public ApiResponse activeDeletedAccount(JwtRequest jwtRequest) throws AlreadyExists, MessagingException {
        User user = userRepository.findByEmail(jwtRequest.getEmail()).orElseThrow(() -> new UsernameNotFoundException("Email not registered with us, please register!!"));

        if(!user.isEnabled()){
            if(user.getStatus()==Status.DELETED) {
                if(passwordEncoder.matches(jwtRequest.getPassword(), user.getPassword())) {
                    UserProfile userProfile = user.getUserProfile();
                    Instant deleteAt = userProfile.getUpdateAt().toInstant().plus(AppConstants.ACCOUNT_DELETION, ChronoUnit.DAYS);
                    if (deleteAt.isAfter(Instant.now())) {
                        String generateOtp = helper.generateOtp(user.getEmail());

                        // sending otp via mail
                        String html = ReactivateAccountOtpTemplate.build(jwtRequest.getEmail(), generateOtp);

                        try {
                            mailMessageProducer.sendEmail(MailMessageDto.builder()
                                    .email(jwtRequest.getEmail())
                                    .subject("Expense Management System || Re-Active Account OTP Verification")
                                    .body(html)
                                    .channel(Channel.EMAIL)
                                    .build());

                        }catch (Exception ex) {
                            log.error("Failed to publish email message", ex);
                            throw new PublishingException("Could not queue email", ex);
                        }

                        otpStore.saveOtp(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()), passwordEncoder.encode(generateOtp));

                        return ApiResponse.builder()
                                .message("OTP sent successfully via email, please enter your OTP to re-active your account")
                                .success(true)
                                .status(HttpStatus.OK)
                                .build();
                    } else {
                        user.getRoles().clear();
                        user.getRefreshTokens().clear();
                        userRepository.deleteById(user.getUserId());
                        throw new UsernameNotFoundException("Account has been permanently deleted, please create a new account!!");
                    }
                }else{
                    throw new BadCredentialsException("Invalid email or password !!");
                }
            }else{
                if(user.getStatus()==Status.ACTIVE || user.getStatus()==Status.FORGET_PASSWORD){
                    throw new IllegalArgumentException("User has not requested to delete his/her account!!");
                }else{
                    throw new IllegalArgumentException("Account does not exist, please register to create an account!!");
                }
            }
        }else{
            throw new AlreadyExists("Account is already active, please login!!");
        }
    }

    @Transactional
    @Scheduled(cron = AppConstants.HALF_HOUR_SCHEDULAR_CRON)
    public void changeUserStatus(){
        List<User> users = userRepository.findByStatusIn(List.of(Status.PENDING, Status.FORGET_PASSWORD));
        if(!users.isEmpty()){
            Instant now = Instant.now();
            for(User user: users){
                if(user.getStatus()==Status.PENDING){
                    Instant createdAt = user.getCreatedAt().toInstant();
                    if(createdAt.plus(AppConstants.EXPIRATION, ChronoUnit.MINUTES).isBefore(now)){
                        user.setStatus(Status.EXPIRED);
                    }
                }else{
                    if(!otpStore.exists(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase())) && !otpStore.isVerified(DigestUtils.sha256Hex(user.getEmail().trim().toLowerCase()))){
                        user.setStatus(Status.ACTIVE);
                    }
                }
            }
            userRepository.saveAll(users);
        }
    }

    @Transactional
    @Scheduled(cron = AppConstants.MIDNIGHT_CRON)
    public void deleteUserFromSystem(){
        List<User> users = userRepository.findByStatusIn(List.of(Status.EXPIRED, Status.DELETED));
        if(!users.isEmpty()){
            List<User> usersToDelete = new ArrayList<>();
            Instant now = Instant.now();
            for(User user: users){
                if(user.getStatus()==Status.DELETED){
                    UserProfile userProfile = user.getUserProfile();
                    Instant updateAt = userProfile.getUpdateAt().toInstant();
                    if(updateAt.plus(AppConstants.ACCOUNT_DELETION,ChronoUnit.DAYS).isBefore(now)){
                        user.getRoles().clear();
                        user.getRefreshTokens().clear();
                        usersToDelete.add(user);
                    }
                }else{
                    user.getRoles().clear();
                    user.getRefreshTokens().clear();
                    usersToDelete.add(user);
                }
            }
            userRepository.deleteAll(usersToDelete);
        }
    }
}

/*
cron understanding:

second (0–59)
│ ┌────────── minute (0–59)
│ │ ┌──────── hour (0–23)
│ │ │ ┌────── day of month (1–31)
│ │ │ │ ┌──── month (1–12)
│ │ │ │ │ ┌── day of week (0–6 or MON–SUN)
* * * * * *

*/

/*
🧠 One table to remember
Environment	                   SameSite	           Secure	          Works?
Local HTTP	                     Lax	           ❌ false	           ✅
Local HTTPS	                     None	           ✅ true	           ✅
Prod (same-site)	             Lax	           ✅ true	           ✅
Prod (cross-site)	             None	           ✅ true	           ✅
Cross-site + Secure=false	     None	           ❌ false	           ❌
 */
