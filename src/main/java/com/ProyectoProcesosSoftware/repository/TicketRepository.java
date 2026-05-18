package com.ProyectoProcesosSoftware.repository;

import com.ProyectoProcesosSoftware.model.Ticket;
import com.ProyectoProcesosSoftware.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByAsistenteId(Long asistenteId);

    List<Ticket> findByAsistenteIdOrderByFechaCompraDesc(Long asistenteId);

    List<Ticket> findByEventoId(Long eventoId);

    boolean existsByEventoIdAndAsistenteId(Long eventoId, Long asistenteId);

    boolean existsByEventoIdAndAsistenteIdAndEstado(
            Long eventoId, Long asistenteId, TicketStatus estado);

            
// US-28 / T-28.2: suma de precioFinal de tickets VALIDO del organizador.
    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(t.precioFinal), 0) " +
        "FROM Ticket t " +
        "WHERE t.evento.organizador.id = :organizadorId " +
        "AND t.estado = com.ProyectoProcesosSoftware.model.TicketStatus.VALIDO")
    java.math.BigDecimal sumarIngresosByOrganizadorId(
            @org.springframework.data.repository.query.Param("organizadorId") Long organizadorId);


}

