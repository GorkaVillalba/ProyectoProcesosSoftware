package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.Evento;
import com.ProyectoProcesosSoftware.model.EstadoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

// T-15 (Persona 3): EventoRepository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    @Query("SELECT e FROM Evento e WHERE e.estado = :estado " +
           "AND (:nombre IS NULL OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
           "AND (:ubicacion IS NULL OR LOWER(e.ubicacion) LIKE LOWER(CONCAT('%', :ubicacion, '%')))")
    Page<Evento> findByEstadoAndFiltros(
            @Param("estado") EstadoEvento estado,
            @Param("nombre") String nombre,
            @Param("ubicacion") String ubicacion,
            Pageable pageable);

    // US-28 / T-28.2: listado de eventos de un organizador (cálculos en servicio)
    List<Evento> findByOrganizadorId(Long organizadorId);

    /**
     * Suma de entradas vendidas de todos los eventos de un organizador.
     */
    @Query("SELECT COALESCE(SUM(e.entradasVendidas), 0) FROM Evento e WHERE e.organizador.id = :organizadorId")
    Long sumEntradasVendidasByOrganizadorId(@Param("organizadorId") Long organizadorId);

    /**
     * Ingresos totales aproximados: SUM(entradasVendidas * precioBase).
     * Se usa precioBase como aproximación del precio de venta histórico.
     */
    @Query("SELECT COALESCE(SUM(e.entradasVendidas * e.precioBase), 0) FROM Evento e WHERE e.organizador.id = :organizadorId")
    BigDecimal sumIngresosByOrganizadorId(@Param("organizadorId") Long organizadorId);

    /**
     * Porcentaje medio de ocupación (entradasVendidas / aforoMaximo * 100)
     * solo sobre eventos con aforoMaximo > 0.
     * Devuelve null si el organizador no tiene eventos.
     */
    @Query("SELECT AVG((e.entradasVendidas * 100.0) / e.aforoMaximo) FROM Evento e " +
           "WHERE e.organizador.id = :organizadorId AND e.aforoMaximo > 0")
    Double avgOcupacionByOrganizadorId(@Param("organizadorId") Long organizadorId);

    /**
     * Número total de eventos de un organizador.
     */
    long countByOrganizadorId(Long organizadorId);
}