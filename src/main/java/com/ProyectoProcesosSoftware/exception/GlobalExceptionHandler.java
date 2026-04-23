package com.ProyectoProcesosSoftware.exception;

import com.ProyectoProcesosSoftware.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicate(DuplicateResourceException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorized(UnauthorizedActionException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(Exception ex) {
        ex.printStackTrace(); // temporal para debug
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
    
    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String message) {
        ErrorResponseDTO error = new ErrorResponseDTO();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(status.value());
        error.setMessage(message);
        return ResponseEntity.status(status).body(error);
    }
}
