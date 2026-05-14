package com.ProyectoProcesosSoftware.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad que representa una reseña de un asistente sobre un evento.
 * <p>
 * La combinación (evento, asistente) es única: cada asistente solo puede
 * dejar una reseña por evento.
 * </p>
 */
@Data
@Entity
@Table(
    name = "resenas",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_resena_evento_asistente",
            columnNames = {"evento_id", "asistente_id"}
        )
    }
)
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asistente_id", nullable = false)
    private Usuario asistente;

    @NotNull(message = "La puntuación es obligatoria")
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    @Column(nullable = false)
    private Integer puntuacion;

    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
    @Column(length = 500)
    private String comentario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}