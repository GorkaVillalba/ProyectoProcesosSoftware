package com.ProyectoProcesosSoftware.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad que representa un evento marcado como favorito por un usuario.
 * <p>
 * La combinación (usuario, evento) es única: un usuario no puede marcar
 * el mismo evento como favorito más de una vez.
 * </p>
 */
@Data
@Entity
@Table(
    name = "favoritos",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_favorito_usuario_evento",
            columnNames = {"usuario_id", "evento_id"}
        )
    }
)
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @PrePersist
    protected void onCreate() {
        this.fechaAlta = LocalDateTime.now();
    }
}