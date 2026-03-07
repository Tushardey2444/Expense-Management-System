package com.manage_expense.security;

import com.manage_expense.entities.User;
import com.manage_expense.entities.UserSession;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtHelper {

//    @Value("${jwt_secret}")
    private static String jwtSecret = "ExpenseManagement12345withSpringBoot2025withJWTkvcihsifvisnbvisbdcvibsivbisbvisbvibsibvisbvibsivbisvbisbvibsivbisdbvisbvibsivb";

    @Value("${jwt_expiration}")
    private long expirationTime; // 1 hour in milliseconds

    private SecretKey secretKey;

    @PostConstruct
    public void init(){
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    public String getEmailFromToken(String token){
        return getClaimFromToken(token, Claims::getSubject);
    }

    public boolean isTokenExpired(String token){
        final Date expiration = getClaimFromToken(token,Claims::getExpiration);
        return expiration.before(new Date());
    }

    public Integer getTokenVersionFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Integer tokenVersion = claims.get("tokenVersion", Integer.class);
        if (tokenVersion == null) {
            throw new JwtException("Missing token_version claim !!");
        }
        return tokenVersion;
    }

    public String getSessionFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        String sessionId = claims.get("sessionId", String.class);
        if (sessionId == null) {
            throw new JwtException("Missing session claim !!");
        }
        return sessionId;
    }

    private <T> T getClaimFromToken(String token, Function<Claims,T> claimResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateToken(User user, UserSession session){
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("tokenVersion", user.getTokenVersion());
        customClaims.put("sessionId", session.getSessionId());
        return doGenerateToken(user.getUsername(), customClaims);
    }

    private String doGenerateToken(String email, Map<String, Object> customClaims){
        return Jwts.builder()
                .claims(customClaims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact();

    }
}
