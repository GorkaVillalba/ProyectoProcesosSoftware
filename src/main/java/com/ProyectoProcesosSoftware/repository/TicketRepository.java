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
}