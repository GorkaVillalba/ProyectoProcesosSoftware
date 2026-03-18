package com.ProyectoProcesosSoftware.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data // Genera getters, setters, toString, etc. por Lombok
@Entity // Indica que esto es una tabla en la BD
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Column(unique = true) // No permite emails repetidos a nivel DB
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    private LocalDateTime fechaRegistro;

    // Se ejecuta automáticamente antes de guardar en la DB
    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
    }
}