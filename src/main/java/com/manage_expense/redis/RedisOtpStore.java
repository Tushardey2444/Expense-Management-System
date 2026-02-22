package com.manage_expense.redis;

import com.manage_expense.config.AppConstants;
import com.manage_expense.exceptions.RedisUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisOtpStore {


    private final RedisTemplate<String, String> redisTemplate;

    public RedisOtpStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /* OTP WRITE */
    @Retry(name = "redisRetry")
    @CircuitBreaker(name = "redisCB", fallbackMethod = "failWrite")
    public void saveOtp(String email, String hashedOtp) {
        redisTemplate.opsForValue()
                .set(AppConstants.OTP_PREFIX + email, hashedOtp, AppConstants.OTP_TTL);
    }

    /* OTP READ + CONSUME (ATOMIC)  */
    @Retry(name = "redisRetry")
    @CircuitBreaker(name = "redisCB", fallbackMethod = "failConsume")
    public String consumeOtp(String email) {
        return redisTemplate.opsForValue()
                .getAndDelete(AppConstants.OTP_PREFIX + email);
    }

    /* READ-ONLY (RETRY SAFE) */
    @Retry(name = "redisRetry")
    @CircuitBreaker(name = "redisCB", fallbackMethod = "failRead")
    public String getOtp(String email) {
        return redisTemplate.opsForValue().get(AppConstants.OTP_PREFIX + email);
    }

    public boolean exists(String email) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(AppConstants.OTP_PREFIX + email));
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            return false;
        }
    }

    public void remove(String email) {
        try {
            redisTemplate.delete(AppConstants.OTP_PREFIX + email);
        } catch (RedisConnectionFailureException | RedisSystemException ignored) {
            // best-effort cleanup
        }
    }

    /* VERIFIED FLAG */
    @Retry(name = "redisOtpRetry")
    @CircuitBreaker(name = "redisOtpCB", fallbackMethod = "failWrite")
    public void markVerified(String email) {
        redisTemplate.opsForValue()
                .set(AppConstants.OTP_VERIFIED_PREFIX + email, "true", AppConstants.OTP_VERIFIED_TTL);
    }

    public boolean isVerified(String email) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(AppConstants.OTP_VERIFIED_PREFIX + email));
        } catch (RedisConnectionFailureException | RedisSystemException e) {
            return false;
        }
    }

    public void clearVerified(String email) {
        try {
            redisTemplate.delete(AppConstants.OTP_VERIFIED_PREFIX + email);
        } catch (RedisConnectionFailureException | RedisSystemException ignored) {
            // best-effort cleanup
        }
    }

    /* FALLBACKS (FAIL CLOSED) */
    private void failWrite(String email, String otp, Throwable ex) {
        throw new RedisUnavailableException("OTP service unavailable", ex);
    }

    private String failConsume(String email, Throwable ex) {
        return null; // OTP validation fail
    }

    private String failRead(String email, Throwable ex) {
        return null; // OTP validation fail
    }
}

/*
# Typical exception chain (Lettuce – default client)
# RedisConnectionFailureException
#  └─ RedisConnectionException
#      └─ ConnectTimeoutException

RedisSystemException: Error in execution
 └─ RedisCommandTimeoutException

RedisConnectionFailureException ->	Cannot connect to Redis
RedisSystemException ->	Redis error during operation
RedisCommandTimeoutException ->	Command took too long
RedisException -> Generic Redis failure

Steps how Resilience4j handles retries and circuit breakers:
1. When a Redis operation fails (throws an exception), Resilience4j intercepts the call.
2. The Retry mechanism attempts to re-execute the operation based on the configured retry policy.
3. If retries are exhausted or not applicable, the Circuit Breaker checks its state:
   - If CLOSED, it allows the call to proceed.
   - If OPEN, it immediately invokes the fallback method.
4. If the call fails while the Circuit Breaker is CLOSED, it records the failure.
5. If failures exceed the threshold, the Circuit Breaker transitions to OPEN state.
6. While OPEN, all calls are short-circuited to the fallback method.
7. After a wait period, the Circuit Breaker transitions to HALF-OPEN, allowing limited calls to test recovery.
8. If a call succeeds, it transitions back to CLOSED; if it fails, it returns to OPEN.

Flow:
Method call → Retry wrapper → CircuitBreaker wrapper → Redis
Retry deals with “try again now”
CircuitBreaker deals with “stop trying for a while”

Detailed step-by-step explanation:
1. Redis operation fails
    “When a Redis operation fails (throws an exception), Resilience4j intercepts the call.”
    Your method is annotated with:
        @Retry(name = "redisRetry")
        @CircuitBreaker(name = "redisCB", fallbackMethod = "fallback")
    Spring AOP intercepts the method call
    Resilience4j wraps the call with Retry + CircuitBreaker decorators
    Redis throws an exception (e.g. connection failure, timeout)

2. Retry attempts to re-execute
    “The Retry mechanism attempts to re-execute the operation based on the configured retry policy.”
    Retry checks:
        Is the exception in retry-exceptions?
        Have max-attempts been exceeded?
        If yes, it immediately retries (or waits, if wait-duration is set)
        Example:
        max-attempts=2
        -> 1 initial call + 1 retry

3. Retries exhausted → CircuitBreaker decision
    “If retries are exhausted or not applicable, the Circuit Breaker checks its state.”
    Now Retry gives up and hands control to CircuitBreaker.
    CircuitBreaker state check:
        CLOSED → allow call to proceed
        OPEN → fail fast, call fallback immediately
        HALF-OPEN → allow limited test calls

4. Failure recorded when CLOSED
    “If the call fails while the Circuit Breaker is CLOSED, it records the failure.”
    CircuitBreaker records:
        Failure count
        Failure rate
    Uses:
        Sliding window (COUNT or TIME based)
        Failure threshold (e.g. 50%)
        This is pure monitoring at this stage.

5. Threshold exceeded → Circuit opens
    “If failures exceed the threshold, the Circuit Breaker transitions to OPEN.”
    Example config:
        failure-rate-threshold=50
        sliding-window-size=10

    -> 5 failures out of last 10 calls
    -> CircuitBreaker transitions to OPEN
    -> CallNotPermittedException is thrown by CircuitBreaker.
    -> Instantly goes to fallback method e.g. failWrite(email, otp, CallNotPermittedException)

6. OPEN state → immediate fallback
    “While OPEN, all calls are short-circuited to the fallback method.”
    Redis is NOT called
    Retry is NOT executed
    Fallback is invoked immediately
    This protects:
        Redis
        Threads
        latency budget

7. Wait period → HALF-OPEN
    “After a wait period, the Circuit Breaker transitions to HALF-OPEN.”
    After:
        wait-duration-in-open-state=10s
    CircuitBreaker allows limited test calls:
        permitted-number-of-calls-in-half-open-state=3
    Purpose: “Is Redis healthy again?”

8. Success or failure decides the next state
    “If a call succeeds, it transitions back to CLOSED; if it fails, it returns to OPEN.”
    Result	      State       transition
    Success	      HALF-OPEN → Circuit CLOSED
    Failure	      HALF-OPEN → Circuit OPEN

    This cycle repeats until Redis stabilizes.

*********** Visual flow ************
Call
 ↓
Retry
 ↓ (fails & retries)
CircuitBreaker
 ├─ CLOSED → try Redis
 ├─ OPEN → fallback immediately
 └─ HALF-OPEN → limited test calls

************ Key clarifications **************
Retry Component    vs      CircuitBreaker Responsibilities
    Retry	                Handle transient failures
    CircuitBreaker	        Prevent cascading failures
    Fallback	            Graceful degradation

    By default:
        @Retry
        @CircuitBreaker
            -> Retry runs inside CircuitBreaker

    Execution order is:
        CircuitBreaker
          └─ Retry
               └─ Redis call

        Retry happens inside CircuitBreaker
        CircuitBreaker sees only the final result and increments failure count (+1) accordingly.
        CircuitBreaker -> One time method invocation = one failure count increment.

    Final takeaway:
        1. Retry = short-term recovery
        2. CircuitBreaker = long-term protection
        3. Together = resilient Redis integration
 */