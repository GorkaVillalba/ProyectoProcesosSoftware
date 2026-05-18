package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.Resena;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    Page<Resena> findByEventoId(Long eventoId, Pageable pageable);

    boolean existsByEventoIdAndAsistenteId(Long eventoId, Long asistenteId);

    @Query("SELECT AVG(r.puntuacion) FROM Resena r WHERE r.evento.id = :eventoId")
    Double findMediaPuntuacionByEventoId(@Param("eventoId") Long eventoId);

    long countByEventoId(Long eventoId);
}