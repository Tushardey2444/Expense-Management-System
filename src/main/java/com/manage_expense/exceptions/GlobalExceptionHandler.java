package com.manage_expense.exceptions;

import com.manage_expense.dtos.dto_responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.password.CompromisedPasswordException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponse> usernameNotFoundExceptionHandler(UsernameNotFoundException exception) {
        log.info("usernameNotFoundException Handler Invoked !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(exception.getMessage())
                .status(HttpStatus.NOT_FOUND)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse> ResponseStatusExceptionHandler(ResponseStatusException exception) {
        log.info("ResponseStatusException Handler Invoked !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(exception.getMessage())
                .status((HttpStatus) exception.getStatusCode())
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, exception.getStatusCode());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse> IllegalStateExceptionHandler(IllegalStateException exception) {
        log.info("IllegalStateException Handler Invoked !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(exception.getMessage())
                .status(HttpStatus.BAD_REQUEST)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> BadCredentialsExceptionHandler(BadCredentialsException exception) {
        log.info("BadCredentialsException Handler Invoked !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(exception.getMessage())
                .status(HttpStatus.BAD_REQUEST)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> ExceptionHandler(Exception exception) {
        log.info("Exception Handler Invoked !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(exception.getMessage())
                .status(HttpStatus.BAD_REQUEST)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(VerifyError.class)
    public ResponseEntity<ApiResponse> VerifyErrorHandler(VerifyError exception) {
        log.info("VerifyError Handler Invoked !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(exception.getMessage())
                .status(HttpStatus.BAD_REQUEST)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CompromisedPasswordException.class)
    public ResponseEntity<ApiResponse> CompromisedPasswordExceptionHandler(CompromisedPasswordException exception) {
        log.info("CompromisedPasswordException Handler Invoked !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(exception.getMessage())
                .status(HttpStatus.BAD_REQUEST)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> IllegalArgumentExceptionHandler(IllegalArgumentException exception) {
        log.info("IllegalArgumentException Handler Invoked !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(exception.getMessage())
                .status(HttpStatus.BAD_REQUEST)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse> IOExceptionHandler(IOException exception) {
        log.info("IOException Handler Invoked !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(exception.getMessage())
                .status(HttpStatus.BAD_REQUEST)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RedisUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<ApiResponse> handleRedisDown(RedisUnavailableException ex) {
        log.info("Redis service temporarily unavailable");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(PublishingException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<ApiResponse> handleRabbitMQDown(PublishingException ex) {
        log.info("RabbitMQ service temporarily unavailable");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(InvalidOtpException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse> handleInvalidOtp(InvalidOtpException ex) {
        log.info("InvalidOtp Exception Handler Invoked !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ResponseEntity<ApiResponse> handleRateLimit(RateLimitExceededException ex) {
        log.info("Too many requests !!");
        ApiResponse responseMessage = ApiResponse.builder()
                .message(ex.getMessage())
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .success(false)
                .build();
        return new ResponseEntity<>(responseMessage, HttpStatus.TOO_MANY_REQUESTS);
    }
}
