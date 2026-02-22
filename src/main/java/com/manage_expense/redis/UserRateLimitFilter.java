package com.manage_expense.redis;

import com.manage_expense.config.AppConstants;
import com.manage_expense.exceptions.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserRateLimitFilter extends OncePerRequestFilter {

    private final UserRedisRateLimiter userRedisRateLimiter;

    public UserRateLimitFilter(UserRedisRateLimiter userRedisRateLimiter) {
        this.userRedisRateLimiter = userRedisRateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try{
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }
            String hashedEmail = DigestUtils.sha256Hex(authentication.getName()); // calls getUsername()

            userRedisRateLimiter.checkUserRateLimit(
                    hashedEmail,
                    AppConstants.USER_RATE_LIMIT_MAX_ATTEMPTS,
                    AppConstants.USER_RATE_LIMIT_TTL
            );
            filterChain.doFilter(request, response);
        }catch (RateLimitExceededException ex){
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                        "error": "Too many requests",
                        "message": "User rate limit exceeded"
                    }
                    """);
        }
    }
}
