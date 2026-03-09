package com.hakyung.barleyssal_spring.global.exception;

import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;
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

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex){
        ErrorCode code = ex.getErrorCode();

        ErrorResponse res = new ErrorResponse(code);

        return  new ResponseEntity<>(res, code.getHttpStatus());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSize(MaxUploadSizeExceededException ex){
        return ResponseEntity
                .badRequest()
                .body("파일 용량이 업로드 가능 범위를 초과하였습니다. (최대 10MB)");
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
