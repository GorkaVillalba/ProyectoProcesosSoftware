package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.Evento;
import com.ProyectoProcesosSoftware.model.EstadoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
