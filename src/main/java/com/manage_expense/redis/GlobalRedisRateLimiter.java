package com.manage_expense.redis;

import org.springframework.stereotype.Service;
import com.manage_expense.config.AppConstants;
import com.manage_expense.exceptions.RateLimitExceededException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;

@Service
public class GlobalRedisRateLimiter {
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public GlobalRedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setResultType(Long.class);
        this.rateLimitScript.setScriptText(
                """
                local current = redis.call('INCR', KEYS[1])
                if tonumber(current) == 1 then
                    redis.call('PEXPIRE', KEYS[1], ARGV[1])
                end
                return current
                """
        );
    }


     // GLOBAL RATE LIMIT CHECK
    public void checkGlobalRateLimit(int maxAttempts, Duration window) {
        Long count = getGlobalRequestCount(window);

        if (count == null || count > maxAttempts) {
            throw new RateLimitExceededException("Global rate limit exceeded", null);
        }
    }

    @Retry(name = "redisRetry")
    @CircuitBreaker(name = "redisCB", fallbackMethod = "redisGlobalUnavailableFallback")
    public Long getGlobalRequestCount(Duration window) {

        return redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(AppConstants.GLOBAL_RATE_LIMIT_PREFIX),
                String.valueOf(window.toMillis())
        );
    }

    /* FALLBACK — REDIS DOWN */
    public Long redisGlobalUnavailableFallback(Duration window, Throwable ex) {
        throw new RateLimitExceededException("Global rate limiting unavailable", ex);
    }
}
