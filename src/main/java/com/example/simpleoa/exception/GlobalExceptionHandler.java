package com.example.simpleoa.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import com.example.simpleoa.common.ApiResponse; // Import ApiResponse
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Removed HashMap and Map imports as they are no longer directly used for response body

/**
 * 全局异常处理器，统一处理来自所有控制器的异常
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        logger.error("Authentication error: ", ex);
        ApiResponse<Object> apiResponse = ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed: " + ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * 处理授权异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.error("Access denied: ", ex);
        ApiResponse<Object> apiResponse = ApiResponse.error(HttpStatus.FORBIDDEN.value(), "Access denied: " + ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }
    
    /**
     * 处理自定义业务异常 (Example - can be expanded)
     * You might have custom exceptions like ResourceNotFoundException, ValidationException etc.
     */
    @ExceptionHandler(RuntimeException.class) // Catching RuntimeException as a general case for now
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime Exception: ", ex);
        // For specific business exceptions, you might return HttpStatus.BAD_REQUEST or other relevant codes
        ApiResponse<Object> apiResponse = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理通用异常 (Fallback)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllExceptions(Exception ex) {
        logger.error("Unexpected error: ", ex);
        ApiResponse<Object> apiResponse = ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred: " + ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
