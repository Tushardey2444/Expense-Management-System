package com.manage_expense.redis;

import com.manage_expense.config.AppConstants;
import com.manage_expense.exceptions.RedisUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisPkceStore {
    private final RedisTemplate<String, String> redisTemplate;

    public RedisPkceStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /* WRITE (PKCE VERIFIER) */
    @Retry(name = "redisRetry")
    @CircuitBreaker(name = "redisCB", fallbackMethod = "failWrite")
    public void save(String state, String verifier) {
        redisTemplate.opsForValue()
                .set(AppConstants.OAUTH_PREFIX + state, verifier, AppConstants.PKCE_TTL);
    }

    /* ONE-TIME USE */
    @Retry(name = "redisRetry")
    @CircuitBreaker(name = "redisCB", fallbackMethod = "failConsume")
    public String consume(String state) {
        return redisTemplate.opsForValue()
                .getAndDelete(AppConstants.OAUTH_PREFIX + state);
    }

    /* CLEANUP (BEST EFFORT) */
    public void remove(String state) {
        try {
            redisTemplate.delete(AppConstants.OAUTH_PREFIX + state);
        } catch (RedisConnectionFailureException | RedisSystemException ignored) {
            // best-effort cleanup
        }
    }

    /* FALLBACKS (FAIL CLOSED – SECURITY CRITICAL) */
    private void failWrite(String state, String verifier, Throwable ex) {
        throw new RedisUnavailableException(
                "PKCE service temporarily unavailable", ex);
    }

    private String failConsume(String state, Throwable ex) {
        return null; // PKCE validation fail
    }
}