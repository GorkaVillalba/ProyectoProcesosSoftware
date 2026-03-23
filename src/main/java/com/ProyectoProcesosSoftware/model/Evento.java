package com.ProyectoProcesosSoftware.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;


@Data
@Entity
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    @NotBlank(message = "La ubicación es obligatoria")
    private String ubicacion;

    @NotNull(message = "El aforo máximo es obligatorio")
    @Min(value = 1)
    private Integer aforoMaximo;

    @Column(nullable = false)
    private Integer entradasVendidas = 0;

    @NotNull(message = "El precio base es obligatorio")
    private BigDecimal precioBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEvento estado = EstadoEvento.BORRADOR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;
}
