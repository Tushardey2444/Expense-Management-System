package com.manage_expense.redis;

import com.manage_expense.config.AppConstants;
import com.manage_expense.exceptions.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private final GlobalRedisRateLimiter globalRedisRateLimiter;

    public GlobalRateLimitFilter(GlobalRedisRateLimiter globalRedisRateLimiter) {
        this.globalRedisRateLimiter = globalRedisRateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            globalRedisRateLimiter.checkGlobalRateLimit(
                    AppConstants.GLOBAL_RATE_LIMIT_MAX_ATTEMPTS,
                    AppConstants.GLOBAL_RATE_LIMIT_TTL
            );
            filterChain.doFilter(request, response);
        }catch (RateLimitExceededException ex){
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                        "error": "Too many requests",
                        "message": "Global rate limit exceeded"
                    }
                    """);
        }
    }
}
