package org.santayn.testing.web.advice;

import jakarta.validation.ConstraintViolationException;
import org.santayn.testing.service.ActiveStudentGroupConflictException;
import org.santayn.testing.service.AuthConflictException;
import org.santayn.testing.service.AuthenticationRateLimitException;
import org.santayn.testing.service.DatabaseBackupException;
import org.santayn.testing.service.ResourceNotFoundException;
import org.santayn.testing.service.ResourceInUseException;
import org.santayn.testing.web.dto.common.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
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
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED", "Validation error", details, trace));
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
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED", "Validation error", details, trace));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", "Missing request parameter: " + ex.getParameterName(), trace));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", "Invalid value for parameter: " + ex.getName(), trace));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        String trace = UUID.randomUUID().toString();
        if (ex.getMessage() != null
                && ex.getMessage().toLowerCase(java.util.Locale.ROOT).contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage(), trace));
        }
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage(), trace));
    }

    @ExceptionHandler(AuthenticationRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationRateLimit(AuthenticationRateLimitException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(ex.getRetryAfterSeconds()))
                .body(ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS.value(), "AUTH_RATE_LIMITED", ex.getMessage(), trace));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(BadCredentialsException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", ex.getMessage(), trace));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Not enough permissions.", trace));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage(), trace));
    }

    @ExceptionHandler(ActiveStudentGroupConflictException.class)
    public ResponseEntity<ErrorResponse> handleActiveStudentGroupConflict(ActiveStudentGroupConflictException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        "STUDENT_GROUP_CONFLICT",
                        ex.getMessage(),
                        List.of(Map.of(
                                "currentGroupId", ex.getCurrentGroupId(),
                                "currentMembershipId", ex.getCurrentMembershipId()
                        )),
                        trace
                ));
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<ErrorResponse> handleResourceInUse(ResourceInUseException ex) {
        String trace = UUID.randomUUID().toString();
        Map<String, Object> detail = new java.util.LinkedHashMap<>();
        detail.put("resourceType", ex.getResourceType());
        detail.put("resourceId", ex.getResourceId());
        detail.putAll(ex.getDependencies());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        "RESOURCE_IN_USE",
                        ex.getMessage(),
                        List.of(detail),
                        trace
                ));
    }

    @ExceptionHandler(AuthConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(AuthConflictException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage(), trace));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "DATA_CONFLICT", "The request conflicts with existing data.", trace));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(
                        HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        "PAYLOAD_TOO_LARGE",
                        "Размер загружаемых файлов превышает допустимый лимит: до 50 МБ на файл и до 200 МБ на запрос.",
                        trace
                ));
    }

    @ExceptionHandler(DatabaseBackupException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseBackup(DatabaseBackupException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), "DATABASE_BACKUP_FAILED", ex.getMessage(), trace));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "Unexpected server error", trace));
    }

    private Map<String, Object> toDetail(FieldError fieldError) {
        return Map.of("field", fieldError.getField(), "issue", fieldError.getDefaultMessage());
    }
}
