package com.ProyectoProcesosSoftware.exception;

import com.ProyectoProcesosSoftware.dto.ErrorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import com.ProyectoProcesosSoftware.dto.ValidationErrorResponseDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("ResourceNotFoundException devuelve 404")
    void handleNotFound_devuelve404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("No encontrado");
        ResponseEntity<ErrorResponseDTO> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("No encontrado");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("DuplicateResourceException devuelve 409")
    void handleDuplicate_devuelve409() {
        DuplicateResourceException ex = new DuplicateResourceException("Ya existe");
        ResponseEntity<ErrorResponseDTO> response = handler.handleDuplicate(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Ya existe");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("UnauthorizedActionException devuelve 403")
    void handleUnauthorized_devuelve403() {
        UnauthorizedActionException ex = new UnauthorizedActionException("Sin permiso");
        ResponseEntity<ErrorResponseDTO> response = handler.handleUnauthorized(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Sin permiso");
        assertThat(response.getBody().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("BusinessRuleException devuelve 409")
    void handleBusinessRule_devuelve409() {
        BusinessRuleException ex = new BusinessRuleException("Regla de negocio violada");
        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessRule(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Regla de negocio violada");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

        @Test
    @DisplayName("MethodArgumentNotValidException devuelve 400 con mapa de errores por campo")
    void handleValidation_devuelve400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError emailError = new FieldError("obj", "email", "El email es obligatorio");
        FieldError passError = new FieldError("obj", "password", "La contraseña debe tener al menos 6 caracteres");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(emailError, passError));

        ResponseEntity<ValidationErrorResponseDTO> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Error de validación");
        assertThat(response.getBody().getErrors())
                .containsEntry("email", "El email es obligatorio")
                .containsEntry("password", "La contraseña debe tener al menos 6 caracteres");
    }

    @Test
    @DisplayName("Exception genérica devuelve 500")
    void handleGeneral_devuelve500() {
        Exception ex = new Exception("Error inesperado");
        ResponseEntity<ErrorResponseDTO> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Error inesperado");
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }
}