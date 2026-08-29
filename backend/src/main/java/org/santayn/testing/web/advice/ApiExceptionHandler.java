package org.santayn.testing.web.advice;

import jakarta.validation.ConstraintViolationException;
import org.santayn.testing.service.AuthConflictException;
import org.santayn.testing.web.dto.common.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(basePackages = "org.santayn.testing.web.controller")
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String trace = UUID.randomUUID().toString();
        List<Map<String, Object>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("validation_failed", "Validation error", details, trace));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String trace = UUID.randomUUID().toString();
        List<Map<String, Object>> details = ex.getConstraintViolations().stream()
                .map(violation -> Map.<String, Object>of(
                        "field", violation.getPropertyPath().toString(),
                        "issue", violation.getMessage()
                ))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("validation_failed", "Validation error", details, trace));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("bad_request", "Missing request parameter: " + ex.getParameterName(), trace));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("bad_request", "Invalid value for parameter: " + ex.getName(), trace));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("bad_request", ex.getMessage(), trace));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(BadCredentialsException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("unauthorized", ex.getMessage(), trace));
    }

    @ExceptionHandler(AuthConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(AuthConflictException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("conflict", ex.getMessage(), trace));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("conflict", "The request conflicts with existing data.", trace));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(
                        "payload_too_large",
                        "Размер загружаемых файлов превышает допустимый лимит: до 50 МБ на файл и до 200 МБ на запрос.",
                        trace
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("internal_error", "Unexpected server error", trace));
    }

    private Map<String, Object> toDetail(FieldError fieldError) {
        return Map.of("field", fieldError.getField(), "issue", fieldError.getDefaultMessage());
    }
}
