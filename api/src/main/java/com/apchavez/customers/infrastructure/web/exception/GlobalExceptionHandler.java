package com.apchavez.customers.infrastructure.web.exception;

import com.apchavez.customers.domain.exception.ClienteDominioInvalidoException;
import com.apchavez.customers.domain.exception.ClienteNoEncontradoException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(ClienteNoEncontradoException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ClienteDominioInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleInvalido(ClienteDominioInvalidoException ex) {
        log.warn("Violación de regla de dominio: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Unprocessable Entity", ex.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("Error de validación — campos inválidos: {}",
                fieldErrors.stream().map(ErrorResponse.FieldError::campo).toList());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.ofValidation(400, "Bad Request", "Error de validación de campos", fieldErrors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        log.warn("Parámetro de ruta inválido: {}", ex.getMessage());
        List<ErrorResponse.FieldError> fieldErrors = ex.getAllErrors().stream()
                .map(e -> new ErrorResponse.FieldError(
                        ex.getMethod().getName(),
                        e.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.ofValidation(400, "Bad Request", "Parámetro inválido", fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Violación de constraint: {}", ex.getMessage());
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(cv -> new ErrorResponse.FieldError(
                        cv.getPropertyPath().toString(),
                        cv.getMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.ofValidation(400, "Bad Request", "Parámetro inválido", fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Error interno no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", "Error interno del servidor"));
    }
}
