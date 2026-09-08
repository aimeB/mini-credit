package com.mini.credit.exception;

import com.mini.credit.dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ApiErrorResponse> handleAuth(AuthException ex) {
    HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.UNAUTHORIZED;
    return ResponseEntity.status(status).body(
            ApiErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(status.value())
                    .code(ex.getCode())
                    .error("AUTH_ERROR")
                    .message(ex.getMessage())
                    .build()
    );
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.NOT_FOUND.value())
                    .code("NOT_FOUND")
                    .error("NOT_FOUND")
                    .message(ex.getMessage())
                    .build()
    );
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .code("BUSINESS_ERROR")
                    .error("BUSINESS_ERROR")
                    .message(ex.getMessage())
                    .build()
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }

    return ResponseEntity.badRequest().body(
            ApiErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
            .code("VALIDATION_ERROR")
                    .error("VALIDATION_ERROR")
                    .message("Erreurs de validation")
                    .validationErrors(errors)
                    .build()
    );
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex) {
    return ResponseEntity.badRequest().body(
            ApiErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
            .code("CONSTRAINT_VIOLATION")
                    .error("CONSTRAINT_VIOLATION")
                    .message(ex.getMessage())
                    .build()
    );
  }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                                                ApiErrorResponse.builder()
                                                                                .timestamp(LocalDateTime.now())
                                                                                .status(HttpStatus.FORBIDDEN.value())
                                                                                .code("ACCESS_DENIED")
                                                                                .error("FORBIDDEN")
                                                                                .message(ex.getMessage())
                                                                                .build()
                );
        }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .code("INTERNAL_SERVER_ERROR")
                    .error("INTERNAL_SERVER_ERROR")
                    .message(ex.getMessage())
                    .build()
    );
  }
}
