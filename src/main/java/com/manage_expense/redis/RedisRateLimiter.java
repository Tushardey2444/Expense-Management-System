package com.manage_expense.redis;

import com.manage_expense.config.AppConstants;
import com.manage_expense.exceptions.RateLimitExceededException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
public class RedisRateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    private final DefaultRedisScript<Long> rateLimitScript;

    public RedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
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

    public void checkRateLimit(String key, int maxAttempts, Duration window) {
        Long count = getRequestCount(key, window);
        if (count == null || count > maxAttempts) {
            throw new RateLimitExceededException("Rate limit exceeded", null);
        }
    }

    @Retry(name = "redisRetry")
    @CircuitBreaker(name = "redisCB", fallbackMethod = "redisUnavailableFallback")
    public Long getRequestCount(String key, Duration window) {
        String redisKey = AppConstants.RATE_LIMIT_PREFIX + key;
        return redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(redisKey),
                String.valueOf(window.toMillis())
        );
    }

    /* FALLBACK — REDIS DOWN */
    public Long redisUnavailableFallback(String key, Duration window, Throwable ex) {
        throw new RateLimitExceededException("Rate limiting unavailable", ex);
    }
}
