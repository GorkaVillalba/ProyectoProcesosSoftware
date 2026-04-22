package com.ProyectoProcesosSoftware.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asistente_id", nullable = false)
    private Usuario asistente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus estado = TicketStatus.VALIDO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioFinal;

    @Column(nullable = false)
    private LocalDateTime fechaCompra;

    @PrePersist
    protected void onCreate() {
        this.uuid = UUID.randomUUID().toString();
        this.fechaCompra = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = TicketStatus.VALIDO;
        }
    }
}