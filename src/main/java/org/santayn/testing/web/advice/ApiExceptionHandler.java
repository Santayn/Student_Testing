package org.santayn.testing.web.advice;

import org.santayn.testing.web.dto.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(basePackages = "org.santayn.testing.web.controller")
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String trace = UUID.randomUUID().toString();
        List<Map<String,Object>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail).toList();
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorResponse("validation_failed", "Validation error", details, trace));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("bad_request", ex.getMessage(), trace));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        String trace = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("internal_error", "Unexpected server error", trace));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        String trace = java.util.UUID.randomUUID().toString();
        String msg = "Data integrity violation";
        // Частый кейс: уникальность name или NOT NULL у faculty_id
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                .body(new ErrorResponse("conflict", msg, java.util.List.of(), trace));
    }


    private Map<String,Object> toDetail(FieldError f) {
        return Map.of("field", f.getField(), "issue", f.getDefaultMessage());
    }
}
