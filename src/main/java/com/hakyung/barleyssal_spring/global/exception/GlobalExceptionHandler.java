package com.hakyung.barleyssal_spring.global.exception;

import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice

public class GlobalExceptionHandler {
    static class ErrorResponse {
        private final int status;
        private final String code;
        private final String message;

        public ErrorResponse(ErrorCode errorCode){
            this.status = errorCode.getHttpStatus().value();
            this.code = errorCode.name();
            this.message = errorCode.getMessage();
        }
        public int getStatus() {return status;}
        public String getCode() {return code;}
        public String getMessage() {return message;}
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(DisabledException ex) {
        return createErrorResponse(ErrorCode.INACTIVE_USER);
    }

    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<ErrorResponse> handleAccountExpiredException(AccountExpiredException ex) {
        return createErrorResponse(ErrorCode.DELETED_ACCOUNT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return createErrorResponse(ErrorCode.AUTH_FAILED);
    }

    public ResponseEntity<ErrorResponse> handleOptimisticLocking(OptimisticLockingFailureException ex) {
        return createErrorResponse(ErrorCode.ALREADY_PROCESSED);
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(ErrorCode errorCode) {
        return new ResponseEntity<>(new ErrorResponse(errorCode), errorCode.getHttpStatus());
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex){
        ErrorCode code = ex.getErrorCode();
        ErrorResponse res = new ErrorResponse(code);
        return  new ResponseEntity<>(res, code.getHttpStatus());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException e) {
        return Map.of(
                "error", "BAD_REQUEST",
                "message", e.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        return ResponseEntity
                .badRequest()
                .body(Map.of("message", message));
    }
}
